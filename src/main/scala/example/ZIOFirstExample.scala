package example

import java.io.IOException

import zio.console.{Console, getStrLn, putStrLn}
import zio.{ExitCode, Has, RIO, Task, URIO, ZIO, ZLayer}

object ZIOFirstExample extends zio.App {

  object weather {
    type WeatherService = Has[Service]
    trait Service {
      def weatherFor(location: String): Task[String]
    }

    def weatherFor(location: String): RIO[WeatherService, String] =
      ZIO.accessM(_.get.weatherFor(location))

    val mock: ZLayer[Any, Nothing, WeatherService] =
      ZLayer.succeed(
        new Service {
          def weatherFor(location: String): Task[String] = Task("sunny as always")
        }
      )

    val live: ZLayer[Any, Nothing, WeatherService] =
      ZLayer.succeed(
        new Service {
          def weatherFor(location: String): Task[String] = location match {
            case "Brno" => Task("stormy")
            case "Bratislava" => Task("sunny")
            case _ => Task("undefined")
          }
        }
      )

  }

  val getCity: ZIO[Console, IOException, String] = putStrLn("Write down city") *> getStrLn

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      city <- getCity
      w <- weather.weatherFor(city).provideLayer(weather.live)
      _ <- putStrLn(s"In $city will be $w")
    } yield city)
      .repeatWhile(_.nonEmpty)
      .exitCode
//      .provideLayer(weather.live ++ Console.live)
//      .provideSomeLayer[Console](weather.live)

}