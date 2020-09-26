package graphql

import caliban.client.SelectionBuilder
import graphql.Client._
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

// Token from https://github.com/ -> Settings -> Developer settings -> Personal access tokens
final case class GithubCfg(token: String)

object GithubClient extends zio.App {

  val token = "86fcf4156dd8c54952b8f34b349db3ba0a7f50e9"

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
