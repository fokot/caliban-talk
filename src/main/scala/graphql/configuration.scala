package graphql

import izumi.reflect.Tag
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfigSource
import zio.{Has, ZIO, ZLayer}

object configuration {

  case class AppConfig(db: DbCfg, github: GithubCfg)

  type Config = Has[AppConfig]

  // handy method to navigate through MyConfig
  def focus[A: Tag](f: AppConfig => A): ZLayer[Config, Nothing, Has[A]] =
    ZLayer.fromService(f)

  val live: ZLayer[Any, ReadError[String], Has[AppConfig]] = ZLayer.fromEffect(
    ZIO.fromEither(
      TypesafeConfigSource.fromDefaultLoader.flatMap(source =>
        read(descriptor[AppConfig] from source)
      )
    )
  )

}
