package example

import caliban.client.SelectionBuilder
import cats.syntax.option._
import graphql.storage.{RepoId, RepoStorage, UserId, UserStorage}
import shapeless.tag
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.read
import zio.config.typesafe.TypesafeConfigSource
import graphql.GithubApi._


case class MyConfig(github: GithubCfg)

// Token from https://github.com/ -> Settings -> Developer settings -> Personal access tokens
final case class GithubCfg(token: String, graphqlUrl: String)

/**
 * Example for caliban-client
 */
object GithubQuery extends zio.App {

/* https://developer.github.com/v4/explorer/

query {
 	search(query: "scala", type: USER, first: 1) {
   nodes {
    	... on Organization {
        id
        login
      	name
        repositories(first: 10) {
					... repos
        }
    	}
    	... on User {
        id
        login
        name
        repositories(first:10) {
					... repos
        }
      }
  	}
	}
}

fragment repos on RepositoryConnection {
  # pageInfo {
  #  endCursor
  # }
  nodes {
    name
    forks(first: 10) {
      nodes {
        id
        name
        owner {
          ... on Organization {
            id
            login
            name
          }
          ... on User {
            id
            login
            name
          }
        }
      }
    }
  }
}
*/

  case class GithubSearchUser(id: UserId, login: String, name: Option[String], repositories: List[GithubRepo])

  case class GithubRepo(id: RepoId, name: String, forks: List[GithubFork])

  case class GithubFork(id: RepoId, name: String, owner: GithubUser)

  case class GithubUser(id: UserId, login: String, name: Option[String])

  val toUserId: String => UserId = tag[UserStorage][String](_)
  val toRepoId: String => RepoId = tag[RepoStorage][String](_)

  def flattenToList[A](as: Option[List[Option[A]]]): List[A] = as.getOrElse(Nil).collect { case Some(gf) => gf }

  val repositoriesBuilder: SelectionBuilder[RepositoryConnection, List[GithubRepo]] =
    RepositoryConnection.nodes(
      (
        Repository.id.map(toRepoId) ~
        Repository.name ~
        Repository.forks(first = 10.some)(
          RepositoryConnection.nodes(
            (
              Repository.id.map(toRepoId) ~
              Repository.name ~
              Repository.owner(
                (User.id.map(toUserId) ~ User.login ~ User.name).some,
                (Organization.id.map(toUserId) ~ Organization.login ~ Organization.name).some,
              ).mapN(GithubUser)
            ).mapN(GithubFork)
          )
        ).map(flattenToList)
      ).mapN(GithubRepo)
    ).map(flattenToList)

  val query = Query.search(first = Some(1), query = "typelevel", `type` = SearchType.USER) {
    SearchResultItemConnection.nodes(
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      (
        Organization.id.map(toUserId) ~
          Organization.login ~
          Organization.name ~
          Organization.repositories(first = 10.some)(repositoriesBuilder)
      ).mapN(GithubSearchUser),
      SelectionBuilder.__typename,
      SelectionBuilder.__typename,
      (User.id.map(toUserId) ~
        User.login ~
        User.name ~
        User.repositories(first = 10.some)(repositoriesBuilder)
      ).mapN(GithubSearchUser),
    ).map(
      _.toList.flatten.collect { case Some(u: GithubSearchUser) => u }
    )
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      config <- ZIO.fromEither(
        TypesafeConfigSource.fromDefaultLoader.flatMap(source =>
          read(descriptor[MyConfig] from source)
        )
      ).map(_.github)
      res <- AsyncHttpClientZioBackend().flatMap { implicit backend =>
        val serverUrl = uri"https://api.github.com/graphql"
        query.toRequest(serverUrl)
          .header("Authorization", s"bearer ${config.token}")
          .send().map(_.body).absolve
      }
    } yield  res
    )
      .tap(a => console.putStrLn(a.toString))
      .tapError(e => console.putStrLn(e.toString))
      .orDie.exitCode

}
