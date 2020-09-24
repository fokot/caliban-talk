package graphql.resolvers.mutations

import graphql.schema.{R, Repo}
import graphql.storage.RepoId

object GCreateFork {

  case class CreateForkInput (
    origin: RepoId,
    fork: RepoId
  )

  case class Input(
    in: CreateForkInput
  )

  case class CreateForkOutput(
    success: Boolean,
    origin: R[Repo],
    fork: R[Repo]
  )

  def mutate(in: CreateForkInput): R[CreateForkOutput] = ???

}
