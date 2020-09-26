package graphql

import caliban.schema.Annotations.GQLDescription
import graphql.Auth.Auth
import graphql.Transactor.TransactorService
import graphql.resolvers.mutations.GCreateFork.CreateForkOutput
import graphql.resolvers.mutations.{GCreateFork, GDeleteFork, GMutateRepo, GMutateUser}
import graphql.resolvers.mutations.GDeleteFork.DeleteForkOutput
import graphql.resolvers.mutations.GMutateRepo.MutateRepoOutput
import graphql.resolvers.mutations.GMutateUser.MutateUserOutput
import graphql.storage.{RepoId, UserId}
import zio.clock.Clock
import zio.console.Console
import zio.query.ZQuery
import zio.random.Random
import zio.stream.ZStream
import zio.{RIO, Task}

object schema {

  type Env = Auth with Clock with TransactorService with Console with Random

  type R[A] = RIO[Env, A]

  type Q[A] = ZQuery[Env, Throwable, A]

  type RStream[A] = ZStream[Env, Throwable, A]

  case class Query(
    token: R[String],
    @GQLDescription("returns user by login")
    user: UserInput => R[User],
    repo: RepoInput => R[Repo],
    repos: R[List[Repo]]
  )

  case class Mutation(
    // you can also have separate create and update mutations if you prefer
    mutateUser: GMutateUser.Input => R[MutateUserOutput],
    mutateRepo: GMutateRepo.Input => R[MutateRepoOutput],
    createFork: GCreateFork.Input => R[CreateForkOutput],
    deleteFork: GDeleteFork.Input => R[DeleteForkOutput],
  )

  case class Subscription(
    repoOfASecond: RStream[Repo]
  )

  case class UserInput(
    login: String
  )

  case class RepoInput(
    owner: String,
    name: String,
  )

  case class User(
    id: UserId,
    login: String,
    name: Option[String],
    repos: R[List[Repo]]
  )

  case class Repo(
    id: RepoId,
    name: String,
    nameWithOwner: Q[String],
    owner: Q[User],
    forkCount: R[Int],
    forks: R[List[Repo]]
  )

}
