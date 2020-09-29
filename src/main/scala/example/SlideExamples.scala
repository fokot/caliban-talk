package example

import io.circe.{Json, ParsingFailure}
import zio.{ExitCode, ZIO, blocking}
import zio.blocking.Blocking
import zio.console.Console

import scala.concurrent.{ExecutionContext, Future}

object SlideExamples {

  type R = Console
  type E = Throwable

  class MyException extends Exception

  case class Repository(id: Int, name: String, owner: String)
  case class User(id: Int, login: String)

  val e1: ZIO[Any, Nothing, Unit] = ZIO.effectTotal(println("aaa"))

  val e2: ZIO[Any, Throwable, Long] = ZIO.effect(scala.Console.in.readLine().toLong)

  def callSomeBlockingApi: String = ???
  val e3: ZIO[Blocking, Throwable, String] = blocking.effectBlocking(callSomeBlockingApi)


  val someEither: Either[MyException, Repository] = ???
  val e4: ZIO[Any, MyException, Repository] = ZIO.fromEither(someEither)

  val someOption: Option[Repository] = ???
  val e5: ZIO[Any, Option[Nothing], Repository] = ZIO.fromOption(someOption)

  def someFuture(ec: ExecutionContext): Future[Repository] = ???
  val e6: ZIO[Any, Throwable, Repository] = ZIO.fromFuture(someFuture)


  val zRepo: ZIO[R, E, Repository] = ???
  val zUser: ZIO[R, E, User] = ???

  val r1: ZIO[R, E, String] =
    zRepo.map(_.name) : ZIO[R, E, String]

  val r2: ZIO[R, E, Int] =
    zRepo.as(42) : ZIO[R, E, Int]

  def isPrivate(r: Repository): ZIO[R, E, Boolean] = ???
  val r3 : ZIO[R, E, Boolean] =
    zRepo.flatMap(isPrivate)

  val r4 : ZIO[R, E, (Repository, User)] =
    zRepo <*> zUser

  val r5 : ZIO[R, E, User] =
    zRepo *> zUser

  val r6 : ZIO[R, E, Repository] =
    zRepo <* zUser

  val r7 : ZIO[R, E, Unit] =
    zRepo.unit

  val r8 : ZIO[R with Console, E, ExitCode] =
    zRepo.exitCode

  import zio.console._

  val c1: ZIO[R, E, Repository] =
    zRepo.tap(r => putStrLn(s"we have repo: ${r.name}"))

  val c2: ZIO[R, E, Repository] =
    zRepo.tapError(e => putStrLn(s"error: ${e.getMessage}"))

  val c3: ZIO[R, E, Repository] =
    zRepo.tapBoth(
      e => putStrLn(s"error: ${e.getMessage}"),
      r => putStrLn(s"we have repo: ${r.name}")
    )

  val getCount: ZIO[R, E, Int] = ???

  val c4: ZIO[R, E, Int] =
    for {
      count <- getCount
      _ <- putStrLn("limit exceeded").when(count > 10)
    } yield count

  val maybeRepo: ZIO[R, E, Option[Repository]] = ???
  val c5: ZIO[R, E, Repository] =
    maybeRepo.someOrFail(new Exception("Repo not found"))

  def parseToJson(s: String): Either[ParsingFailure, Json] = ???

  val c6: ZIO[Any, ParsingFailure, Json] =
    ZIO.succeed("""{"name": "zio"}""").map(parseToJson).absolve

}
