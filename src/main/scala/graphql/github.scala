package graphql

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import cats.syntax.option._
import graphql.storage.{ForkStorage, RepoId, RepoStorage, UserId, UserStorage}
import shapeless.tag
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.SttpClient
import zio._
import zio.duration._
import graphql.GithubApi._
import zio.clock.Clock
import zio.console.Console

// Token from https://github.com/ -> Settings -> Developer settings -> Personal access tokens
final case class GithubCfg(token: String, graphqlUrl: String)

object github {

  type GithubService = Has[Service]

  type GithubImport = (Set[UserStorage], Set[RepoStorage], Set[ForkStorage])

  trait Service {
    def importFromGithub(query: String): RIO[Clock with Console, GithubImport]
  }

  def importFromGithub(query: String): RIO[GithubService with Clock with Console, GithubImport] =
    ZIO.accessM(_.get.importFromGithub(query))

  val live: ZLayer[Has[GithubCfg] with SttpClient, Nothing, GithubService] =
    ZLayer.fromServices[GithubCfg, SttpClient.Service, Service](
      new LiveService(_, _)
    )

  class LiveService(config: GithubCfg, sttpClient: SttpClient.Service) extends Service {
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

    def searchQuery(query: String): SelectionBuilder[RootQuery, List[GithubSearchUser]] =
      Query.search(first = Some(1), query = query, `type` = SearchType.USER) {
        SearchResultItemConnection.nodes(
          SelectionBuilder.__typename,
          SelectionBuilder.__typename,
          SelectionBuilder.__typename,
          (
            Organization.id.map(toUserId) ~
              Organization.login ~
              Organization.name ~
              Organization.repositories(first = 30.some)(repositoriesBuilder)
            ).mapN(GithubSearchUser),
          SelectionBuilder.__typename,
          SelectionBuilder.__typename,
          (User.id.map(toUserId) ~
            User.login ~
            User.name ~
            User.repositories(first = 30.some)(repositoriesBuilder)
            ).mapN(GithubSearchUser),
        ).map(
          _.toList.flatten.collect { case Some(u: GithubSearchUser) => u }
        )
      }

    def toGithubImport(us: List[GithubSearchUser]): GithubImport =
      (
        us.map(u => UserStorage(u.id, u.login, u.name)).toSet ++
          us.flatMap(_.repositories.flatMap(_.forks)).map(_.owner).map(u => UserStorage(u.id, u.login, u.name)).toSet,
        us.flatMap(u => u.repositories.map(r => RepoStorage(r.id, r.name, u.id))).toSet ++
          us.flatMap(_.repositories.flatMap(_.forks)).map(r => RepoStorage(r.id, r.name, r.owner.id)).toSet,
        us.flatMap(_.repositories.flatMap(r => r.forks.map(f => ForkStorage(r.id, f.id)))).toSet
      )

    val schedule = Schedule.exponential(10.milliseconds) && Schedule.recurs(20)

    import zio.console._

    override def importFromGithub(query: String): RIO[Clock with Console, GithubImport] =
      sttpClient.send(
          searchQuery(query).toRequest(uri"${config.graphqlUrl}")
            .header("Authorization", s"bearer ${config.token}")
      ).map(_.body).absolve.map(toGithubImport)
        .tapBoth(_ => putStrLn("import fail"), _ => putStrLn("import succeed"))
        .retry(schedule)
        .timeoutFail(new Exception(s"Github import for $query timed out"))(20.seconds)

  }

}