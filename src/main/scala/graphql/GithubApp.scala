package graphql

import caliban.GraphQL._
import caliban.schema.GenericSchema
import caliban.{Http4sAdapter, RootResolver}
import cats.data.Kleisli
import cats.effect.Blocker
import graphql.Auth.Auth
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, ServiceErrorHandler}
import org.http4s.util.CaseInsensitiveString
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.concurrent.ExecutionContext

object GithubApp extends CatsApp {

  type AuthTask[A] = RIO[Auth, A]

  case class MissingToken() extends Throwable

  // http4s middleware that extracts a token from the request and eliminate the Auth layer dependency
  object AuthMiddleware {
    def apply(route: HttpRoutes[AuthTask]): HttpRoutes[Task] =
      Http4sAdapter.provideLayerFromRequest(
        route,
        req =>
          ZLayer.succeed(Auth.fromToken(req.headers.get(CaseInsensitiveString("Authorization")).map(_.value)))
      )
  }

  // http4s error handler to customize the response for our throwable
  object dsl extends Http4sDsl[Task]
  import dsl._
  val errorHandler: ServiceErrorHandler[Task] = _ => { case MissingToken() => Forbidden() }

  // our GraphQL API
  val schema: GenericSchema[Auth] = new GenericSchema[Auth] {}
  import schema._
  case class Query(token: RIO[Auth, String])
  private val resolver = RootResolver(Query(ZIO.access[Auth](_.get[Auth.Service].token.getOrElse(""))))
  private val api      = graphQL(resolver)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      interpreter <- api.interpreter
      blocker     <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
      _ <- BlazeServerBuilder[Task](ExecutionContext.global)
        .withServiceErrorHandler(errorHandler)
        .bindHttp(8088, "localhost")
        .withHttpApp(Router[Task](
            "/api/graphql" -> CORS(AuthMiddleware(Http4sAdapter.makeHttpService(interpreter))),
            "/ws/graphql"  -> CORS(AuthMiddleware(Http4sAdapter.makeWebSocketService(interpreter))),
            "/graphiql"    -> Kleisli.liftF(StaticFile.fromResource("/graphiql.html", blocker, None))
          ).orNotFound
        )
        .resource
        .toManaged
        .useForever
    } yield ()).exitCode
}