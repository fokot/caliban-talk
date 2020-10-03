package graphql

import java.io.File
import java.util.regex.Pattern

import caliban.Value.NullValue
import caliban._
import cats.syntax.option._
import graphql.auth.AuthUser
import graphql.configuration.AppConfig
import graphql.schema.{Env, EnvWithoutAuth}
import io.circe.{Json, JsonObject}
import io.circe.syntax._
import zio.ExecutionStrategy.Sequential
import zio._
import zio.blocking.Blocking
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, Spec, TestFailure, TestSuccess, ZSpec, assert, testM}
import zio.interop.catz._

import scala.io.Source

trait GQLTestBase extends DefaultRunnableSpec {

  case class GQLReq(fileName: String, query: String, variables: Map[String, InputValue], result: Json, authUser: AuthUser)

  private val quotes = "```"
  private val quotesJson = "```json"

  private def findBlock(
      fileName: String,
      lines: List[String],
      blockName: String,
      separatorStart: String,
      separatorEnd: String
  ): Task[Option[String]] = {
    val block = lines.dropWhile(_ != s"### ${blockName}:").drop(1).takeWhile(l => !l.startsWith("###"))
    val quotesStartNumber = block.count(_ == separatorStart)
    val quotesEndNumber = block.count(_ == separatorEnd)
    if (block.isEmpty || (quotesStartNumber == 0 && quotesEndNumber == 0))
      ZIO.none
    else if (if (separatorStart == separatorEnd) quotesStartNumber != 2 || quotesEndNumber != 2
             else quotesStartNumber != 1 || quotesEndNumber != 1)
      ZIO.fail(
        new Exception(s"${fileName}: wrong number of quotes for ${blockName} $quotesStartNumber $quotesEndNumber \n${block.mkString("\n")}")
      )
    else
      ZIO.effect(block.dropWhile(_ != separatorStart).drop(1).takeWhile(_ != separatorEnd).mkString("\n").some)
  }

  private def readFile(path: String): Task[List[String]] =
    Managed.fromAutoCloseable(Task.effect(Source.fromFile(path))).use(s => Task.effect(s.getLines().toList))

  private val authPattern = Pattern.compile(""".*\[Auth\]\(([^\)]+)\).*""")

  private def findAuth(fileName: String, lines: List[String]): Task[Option[String]] = {
    val link = lines.find(l => authPattern.matcher(l).matches()).map(l => { val m = authPattern.matcher(l); m.matches(); m.group(1) })
    ZIO.foreach(link)(p => readFile(s"${fileName.split("/").dropRight(1).mkString("/")}/$p").map(_.mkString("\n")))
  }

  private def gqlReq(file: File): Task[GQLReq] =
    for {
      lines <- readFile(file.getAbsolutePath)
      fileName = file.getAbsolutePath
      authData <- findAuth(fileName, lines)
        .someOrFail(new Exception(s"${fileName}: no auth"))
        .flatMap(s => Task.effect(io.circe.parser.decode[AuthUser](s)).absolve)
      query <- findBlock(fileName, lines, "Query", quotes, quotes).someOrFail(new Exception(s"${fileName}: no query"))
      variables <- findBlock(fileName, lines, "Variables", quotesJson, quotes)
      variablesParsed <- variables
        .map(
          s =>
            Task
              .effect(io.circe.parser.parse(s).toOption.flatMap(_.as[Map[String, InputValue]].toOption))
              .someOrFail(new Exception(s"${fileName}: error parsing variables"))
        )
        .getOrElse(ZIO.succeed(Map.empty[String, InputValue]))
      result <- findBlock(fileName, lines, "Result", quotesJson, quotes).someOrFail(new Exception(s"${fileName}: no result"))
      resultParsed <- ZIO.effect(io.circe.parser.parse(result).toOption).someOrFail(new Exception(s"${fileName}: error parsing result"))
    } yield GQLReq(file.getName, query, variablesParsed, resultParsed, authData)

  val config =
    AppConfig(
      // db is from docker
      DbCfg("", "", "", ""),
      // not using gh in tests
      GithubCfg("", "")
    )

  def uri: String


  // if you have common setup you wan to run before every test case, uncomment this
  //  val commonPreTests = getClass.getClassLoader.getResource("common-pre-tests").getPath
  val files = /* new File(commonPreTests).listFiles.toList.sortBy(_.getName) ++ */
    new File(uri).listFiles.filterNot(_.getName == "README.md").sortBy(_.getName).toList

  def noKeyRes(key: String) = new Exception(s"No key $key in response")

  def noKeyStored(key: String) = new Exception(s"No key $key stored")

  // return expected Json with replaced stored values and updates storedValues
  def replaceAndStore(expected: Json, res: Json, storedValues: Ref[Map[String, Json]]): Task[Json] =
    (expected.asObject, res.asObject) match {
      case (Some(expectedObj), Some(resObj)) => {
        expectedObj.toMap.foldLeft[Task[List[(String, Json)]]](Task(List.empty)) (
          (acc, elem) => acc.flatMap{accList =>
            val (key, value) = elem
            if(value.asString.exists(_.startsWith(">>>")))
              for {
                valueFromRes <- Task(resObj(key)).someOrFail(noKeyRes(key))
                _ <- storedValues.update(
                  _.updated(value.asString.get.substring(3), valueFromRes)
                )
              } yield (key, valueFromRes) :: accList
            else if(value.asString.exists(_.startsWith("<<<")))
              for {
                valueFromStore <- storedValues.get.map(_.get(value.asString.get.substring(3))).someOrFail(noKeyStored(value.noSpaces.substring(3)))
              } yield (key, valueFromStore) :: accList
            else if(value.isObject)
              for {
                valueFromRes <- Task(resObj(key)).someOrFail(noKeyRes(key))
                replacedJson <- replaceAndStore(value.asJson, valueFromRes, storedValues)
              } yield (key, replacedJson) :: accList
            else Task(elem :: accList)
          }
        ).map(JsonObject.fromIterable).map(_.asJson)
      }
      case _ => Task(expected)
    }

  def replace(expected: Json, storedValues: Ref[Map[String, Json]]): Task[Json] =
    expected.asObject.map(
      _.traverse(j =>
        if(j.asString.exists(_.startsWith("<<<")))
          storedValues.get.map(_.get(j.asString.get.substring(3))).someOrFail(noKeyStored(j.asString.get.substring(3)))
        else if(j.isObject)
          replace(j, storedValues)
        else
          Task(j)
      ).map(_.asJson)
    ).getOrElse(Task(expected))


  // this Task is needed only because of ability to store and use values between tests, if you don't need it simplify it
  def reqToTest(interpreter: GraphQLInterpreter[Env, CalibanError], storedValues: Ref[Map[String, Json]])(req: GQLReq): ZSpec[EnvWithoutAuth, Nothing] =
    testM(s"Requests ${req.fileName} should pass")(
          ( for {
            variablesReplaced <- replace(req.variables.asJson, storedValues)
            variables <- Task.effect(variablesReplaced.as[Map[String, InputValue]].toOption).someOrFail(new Exception(s"Error parsing replaced variables in ${req.fileName}"))
            res <- interpreter
              .executeRequest(GraphQLRequest(req.query.some, None, variables.some))
              .foldCause(cause => GraphQLResponse(NullValue, cause.defects).asJson, _.asJson)
            expectedReplaced <- replaceAndStore(req.result, res, storedValues)

          } yield assert(res)(equalTo(expectedReplaced))
        ).orDie
        .provideSomeLayer[EnvWithoutAuth](
          ZLayer.succeed[auth.Service](
            new auth.Service {
              override def token: Option[String] = req.authUser.asJson.noSpaces.some

              override def user: Option[AuthUser] = req.authUser.some
            }
          )
        )
    )

  val tests: ZIO[EnvWithoutAuth, TestFailure[Throwable], List[Spec[EnvWithoutAuth, TestFailure[Throwable], TestSuccess]]] = (for {
    requests <- ZIO.foreach(files)(gqlReq)
    interpreter <- resolver.interpreter
    storedValues <- Ref.make[Map[String, Json]](Map.empty)
    test = requests.map(reqToTest(interpreter, storedValues))
  } yield test).mapError(e => TestFailure.Runtime(Cause.die(e)))

  val testLayer =
    ZLayer.succeed(config) ++ (Blocking.live >>> EmbeddedPostgresLayer.transactorServiceLayer) ++ github.dummy

  def spec: Spec[TestEnvironment, TestFailure[Throwable], TestSuccess] =
    Spec
      .suite(getClass.getSimpleName, EmbeddedPostgresLayer.withSchema *> tests.map(_.toVector).toManaged_, Sequential.some)
      .provideCustomLayerShared(testLayer)
}
