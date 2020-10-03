package graphql

import caliban.execution.ExecutionRequest
import caliban.Http4sAdapter
import caliban.wrappers.Wrapper.ExecutionWrapper
import cats.data.Kleisli
import cats.effect.Blocker
import graphql.auth.Auth
import graphql.Transactor.{DataSourceService, TransactorService}
import graphql.configuration.Config
import graphql.github.GithubService
import graphql.schema.{Env, EnvWithoutAuth, R}
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, ServiceErrorHandler}
import org.http4s.util.CaseInsensitiveString
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._

import scala.concurrent.ExecutionContext

object CalibanApp extends CatsApp {

  type F[A] = RIO[EnvWithoutAuth, A]

  case class MissingToken() extends Throwable

  // http4s middleware that extracts a token from the request and eliminate the Auth layer dependency
  object AuthMiddleware {
    def apply(route: HttpRoutes[R]): HttpRoutes[F] =
      Http4sAdapter.provideSomeLayerFromRequest[EnvWithoutAuth, Auth](
        route,
        req =>
          ZLayer.succeed(auth.fromToken(req.headers.get(CaseInsensitiveString("Authorization")).map(_.value)))
      )
  }

  // http4s error handler to customize the response for our throwable
  object dsl extends Http4sDsl[F]
  import dsl._
  val errorHandler: ServiceErrorHandler[F] = _ => { case MissingToken() => Forbidden() }


  val customLayer: ZLayer[Blocking with Clock, Throwable, Config with DataSourceService with TransactorService with GithubService] =
    ZLayer.identity[Blocking] ++
      ZLayer.identity[Clock] ++
      configuration.live >+>
      configuration.focus(_.db) >+>
  // uncomment this and comment `dockerLayer` to run against local postres
//      Transactor.transactorLayer >+>
      Transactor.dockerLayer >+>
      configuration.focus(_.github) >+>
      AsyncHttpClientZioBackend.layer() >+>
      github.live

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZIO
      .runtime[EnvWithoutAuth]
      .flatMap(implicit runtime =>
        for {
          _ <- Transactor.runFlywayMigrations()
          interpreter <- resolver.interpreter
          blocker     <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
          _ <- BlazeServerBuilder[F](ExecutionContext.global)
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
      .provideCustomLayer(customLayer)
      .exitCode
}