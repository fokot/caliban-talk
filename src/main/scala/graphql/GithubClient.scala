package graphql

import caliban.client.SelectionBuilder
import graphql.Client._
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

object GithubClient extends zio.App {

  // Token from https://github.com/ -> Settings -> Developer settings -> Personal access tokens
  val token = "6dfddc6d8382cee0a8670ab89abb4725ec418253"

  case class Repo(name: String, createdAt: DateTime, forkCount: Int)

  val query = Query.search(first = Some(10), query = "zio", `type` = SearchType.REPOSITORY) {
    SearchResultItemConnection.nodes(
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      (Repository.name ~ Repository.createdAt ~ Repository.forkCount).mapN(Repo),
      SelectionBuilder.__typename,
    ).map(
      _.toList.flatten.collect { case Some(r: Repo) => r }
    )
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    AsyncHttpClientZioBackend().flatMap { implicit backend =>
      val serverUrl = uri"https://api.github.com/graphql"
      query.toRequest(serverUrl)
        .header("Authorization", s"bearer $token")
        .send().map(_.body).absolve
    }
      .tap(a => console.putStrLn(a.toString))
      .tapError(e => console.putStrLn(e.toString))
      .orDie.exitCode

}
