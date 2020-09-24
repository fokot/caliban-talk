package graphql

import cats.effect.Blocker
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.{Transactor => DoobieTransactor}
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, UIO, ZIO, ZLayer}

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

  def transactorLayer: ZLayer[Blocking with Has[DbCfg], Throwable, TransactorService] =
    (ZLayer.identity[Blocking] ++ hikariDataSourceLayer) >>> hikariTransactorLayer

}
