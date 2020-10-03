package graphql

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import graphql.Transactor.{DataSourceService, TransactorService}
import izumi.reflect.Tag
import org.flywaydb.core.Flyway
import zio.ExecutionStrategy.Sequential
import zio.blocking.Blocking
import zio.test.TestAspect.before
import zio.test.environment.TestEnvironment
import zio.test.{Spec, TestFailure, TestSuccess}
import zio._

object EmbeddedPostgresLayer {

  lazy val cfgLocal = {
    val cfg = new HikariConfig()
    cfg.setDriverClassName("org.postgresql.Driver")
    cfg.setJdbcUrl("jdbc:postgresql://localhost:5432/upstart?currentSchema=rar")
//    cfg.setJdbcUrl("jdbc:postgresql://localhost/postgres")
    cfg.setSchema("rar")
    cfg.setUsername("admin")
    cfg.setPassword("1234")
    cfg
  }

  private val localDataSourceLayer: ZLayer[Any, Throwable, DataSourceService] =
    ZLayer.fromAcquireRelease(ZIO.effect(new HikariDataSource(cfgLocal)))(ds => UIO(ds.close()))

  val transactorServiceThrowableLayer: ZLayer[Blocking, Throwable, DataSourceService with TransactorService] =
    Transactor.dockerLayer
//   CONNECT TO LOCAL DB
//    (ZLayer.identity[Blocking] ++ localDataSourceLayer) >>> TransactorFactory.hikariTransactorLayer

  val transactorServiceLayer: ZLayer[Blocking, TestFailure.Runtime[Nothing], DataSourceService with TransactorService] =
    transactorServiceThrowableLayer.mapError(t => TestFailure.Runtime(Cause.die(t)))

  val dropSchema: RIO[DataSourceService, Unit] =
    ZIO.accessM[DataSourceService](
      r =>
        ZIO.effect {
          val conn = r.get.getConnection
          conn.prepareCall(s"""DROP SCHEMA IF EXISTS ${r.get.getSchema} CASCADE;""").execute()
          conn.close()
        }
    )

  val prepareDb: RIO[DataSourceService, Unit] = dropSchema *> Transactor.runFlywayMigrations()

  val withSchemaThrowable: ZManaged[DataSourceService, Throwable, Unit] = ZManaged.make(prepareDb)(_ => dropSchema.orDie)

  val withSchema: ZManaged[DataSourceService, TestFailure.Runtime[Nothing], Unit] =
    withSchemaThrowable.mapError(e => TestFailure.Runtime(Cause.die(e)))

//  val transactorServiceLayerWithMigrations: ZLayer[Blocking, TestFailure.Runtime[Nothing], DataSourceService with TransactorService] =
//    transactorServiceLayer < ZLayer.fromManaged(withSchema)

  /**
    * Run tests in sequence on single db and recreates schema between them
    * It is ~2x faster and for sure less memory than creating container for each test and using `provideCustomLayer`
    */
  def sequentialSuite[R <: Has[_]: Tag](
      className: String,
      r: Layer[Nothing, R],
      tests: List[Spec[R with DataSourceService with TransactorService with TestEnvironment, TestFailure[Throwable], TestSuccess]]
  ): Spec[TestEnvironment, TestFailure[Throwable], TestSuccess] =
    Spec
      .suite[R with DataSourceService with TransactorService with TestEnvironment, TestFailure[Throwable], TestSuccess](
        className,
        ZManaged.succeed(tests.map(_ @@ before(EmbeddedPostgresLayer.prepareDb.orDie)).toVector),
        Some(Sequential)
      )
      .provideCustomLayerShared(r ++ EmbeddedPostgresLayer.transactorServiceLayer)
}
