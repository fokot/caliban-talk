package graphql

import cats.effect.Blocker
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.{Transactor => DoobieTransactor}
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, RIO, Task, UIO, ZIO, ZLayer, ZManaged}

final case class DbCfg(schema: String, url: String, username: String, password: String)

object Transactor {

  def hikariConfig(c: DbCfg): HikariConfig = {
    val cfg = new HikariConfig()
    //cfg.setDriverClassName("org.postgresql.ds.PGSimpleDataSource")
    cfg.setDriverClassName("org.postgresql.Driver")
    cfg.setJdbcUrl(c.url)
    cfg.setSchema(c.schema)
    cfg.setUsername(c.username)
    cfg.setPassword(c.password)
    cfg
  }

  type DataSourceService = Has[HikariDataSource]
  type TransactorService = Has[DoobieTransactor[Task]]

  def hikariDataSourceLayer: ZLayer[Has[DbCfg], Throwable, DataSourceService] =
    ZLayer.fromAcquireRelease(ZIO.access[Has[DbCfg]](_.get).flatMap(cfg => ZIO.effect(new HikariDataSource(hikariConfig(cfg)))))(
      ds => UIO(ds.close())
    )

  def hikariTransactorLayer: ZLayer[Blocking with DataSourceService, Nothing, TransactorService] =
    ZLayer.fromEffect(for {
      ds <- ZIO.access[Has[HikariDataSource]](_.get)
      blockingEC <- ZIO.access[Blocking](_.get.blockingExecutor.asEC)
      transactor <- ZIO.runtime[Any].map { implicit rt =>
        HikariTransactor[Task](ds, rt.platform.executor.asEC, Blocker.liftExecutionContext(blockingEC))
      }
    } yield transactor)

  def transactorLayer: ZLayer[Blocking with Has[DbCfg], Throwable, DataSourceService with TransactorService] =
    (ZLayer.identity[Blocking] ++ hikariDataSourceLayer) >+> hikariTransactorLayer

  def runFlywayMigrations(migrationsDir: String = "migration"): RIO[DataSourceService, Unit] =
    ZIO.accessM[DataSourceService](
      r =>
        ZIO.effect {
          val fw = Flyway.configure().dataSource(r.get).schemas(r.get.getSchema).locations(migrationsDir).load()
          fw.migrate()
        }.unit
    )

  // CONTAINER LAYER

  def cfgFromContainer(container: PostgreSQLContainer) = {
    val cfg = new HikariConfig()
    cfg.setDriverClassName(container.driverClassName)
    cfg.setJdbcUrl(container.jdbcUrl)
    cfg.setUsername(container.username)
    cfg.setPassword(container.password)
    cfg.setSchema("public")
    cfg
  }

  val containerLayer: ZLayer[Any, Throwable, Has[PostgreSQLContainer]] = ZLayer.fromManaged(ZManaged.makeEffect {
    val container: PostgreSQLContainer = PostgreSQLContainer(dockerImageNameOverride = DockerImageName.parse("postgres:alpine"))
    container.start()
    container
  }(_.stop()))

  val dataSourceLayer: ZLayer[Has[PostgreSQLContainer], Throwable, Has[HikariDataSource]] =
    ZLayer.fromAcquireRelease(ZIO.accessM[Has[PostgreSQLContainer]](r => ZIO.effect(new HikariDataSource(cfgFromContainer(r.get)))))(
      ds => UIO(ds.close())
    )

  val dockerLayer: ZLayer[Blocking, Throwable, DataSourceService with TransactorService] =
    (ZLayer.identity[Blocking] ++ (containerLayer >>> dataSourceLayer)) >+> Transactor.hikariTransactorLayer

}
