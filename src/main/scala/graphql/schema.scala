package graphql

import graphql.Auth.Auth
import zio.clock.Clock
import zio.stream.ZStream
import zio.{RIO, Task}

object schema {

  type Env = Auth with Clock

  type R[A] = RIO[Env, A]

  type RStream[A] = ZStream[Env, Throwable, A]

  case class Query(
    token: R[String],
    user: UserArgs => R[User],
    repo: RepoArgs => R[Repo],
    repos: R[List[Repo]]
  )

  case class Mutation(
    //FIXME
    addRepo: R[String]
  )

  case class Subscription(
    repoOfASecond: RStream[String]
  )

  case class UserArgs(
    login: String
  )

  case class RepoArgs(
    owner: String,
    name: String,
  )

  case class User(
    id: String,
    login: String,
    name: String,
    repos: Task[List[Repo]]
  )

  case class Repo(
    id: String,
    name: String,
    nameWithOwner: String,
    owner: Task[User],
    forks: Task[List[Repo]]
  )

}
