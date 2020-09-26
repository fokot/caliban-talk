package graphql.resolvers.mutations

import graphql.resolvers.GRepo
import graphql.schema.{R, Repo}
import graphql.storage
import graphql.storage.{ForkStorage, RepoId}

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

  def mutate(in: CreateForkInput): R[CreateForkOutput] =
    storage.createFork(ForkStorage(in.origin, in.fork)) as
      CreateForkOutput(
        true,
        GRepo.byId(in.origin),
        GRepo.byId(in.fork)
      )


}
