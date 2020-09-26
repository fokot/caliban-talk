package graphql

import caliban.execution.ExecutionRequest
import caliban.{GraphQLRequest, Http4sAdapter}
import caliban.wrappers.Wrapper.{ExecutionWrapper, OverallWrapper}
import cats.data.Kleisli
import cats.effect.{Blocker, ConcurrentEffect, Timer}
import graphql.Auth.Auth
import graphql.Transactor.TransactorService
import graphql.schema.Env
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, ServiceErrorHandler}
import org.http4s.util.CaseInsensitiveString
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.random.Random

import scala.concurrent.ExecutionContext

object GithubApp extends CatsApp {

  type F[A] = RIO[Clock with TransactorService with Console with Random, A]
  type AuthTask[A] = RIO[Env, A]

  case class MissingToken() extends Throwable

  // http4s middleware that extracts a token from the request and eliminate the Auth layer dependency
  object AuthMiddleware {
    def apply(route: HttpRoutes[AuthTask]): HttpRoutes[F] =
      Http4sAdapter.provideSomeLayerFromRequest[Clock with TransactorService with Console with Random, Auth](
        route,
        req =>
          ZLayer.succeed(Auth.fromToken(req.headers.get(CaseInsensitiveString("Authorization")).map(_.value)))
      )
  }

  // http4s error handler to customize the response for our throwable
  object dsl extends Http4sDsl[F]
  import dsl._
  val errorHandler: ServiceErrorHandler[F] = _ => { case MissingToken() => Forbidden() }

  // error logger wrapper
  val wrapper = ExecutionWrapper[Env] { process => (request: ExecutionRequest) => process(request).tap(
    r => ZIO.foreach_(r.errors)(e => zio.console.putStrLn(e.toString))
  )  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZIO
      .runtime[ZEnv with TransactorService]
      .flatMap(implicit runtime =>
        for {
          interpreter <- resolver.api.withWrapper(wrapper).interpreter
          blocker     <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
          _ <- BlazeServerBuilder[F](ExecutionContext.global)(ConcurrentEffect[Task].asInstanceOf[ConcurrentEffect[F]], Timer[Task].asInstanceOf[Timer[F]])
            .withServiceErrorHandler(errorHandler)
            .bindHttp(8088, "localhost")
            .withHttpApp(Router[F](
              "/api/graphql" -> CORS(AuthMiddleware(Http4sAdapter.makeHttpService(interpreter))),
              "/ws/graphql"  -> CORS(AuthMiddleware(Http4sAdapter.makeWebSocketService(interpreter))),
              "/graphiql"    -> Kleisli.liftF(StaticFile.fromResource("/graphiql.html", blocker, None))
            ).orNotFound
            )
            .resource
            .toManaged
            .useForever
        } yield ()
      )
      .provideCustomLayer(ZLayer.succeed(DbCfg("caliban_talk", "jdbc:postgresql://localhost:5432/upstart", "admin", "1234")) ++ ZLayer.identity[Blocking] >>> Transactor.transactorLayer)
      .exitCode
}