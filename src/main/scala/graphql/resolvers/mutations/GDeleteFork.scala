package graphql.resolvers.mutations

import graphql.resolvers.GRepo
import graphql.schema.{R, Repo}
import graphql.storage
import graphql.storage.{ForkStorage, RepoId}

object GDeleteFork {

  case class DeleteForkInput(
    origin: RepoId,
    fork: RepoId
  )

  case class Input(
    in: DeleteForkInput
  )

  case class DeleteForkOutput(
    success: Boolean,
    origin: R[Repo]
  )

  def mutate(in: DeleteForkInput): R[DeleteForkOutput] =
    storage.deleteFork(ForkStorage(in.origin, in.fork)) as
      DeleteForkOutput(
        true,
        GRepo.byId(in.origin)
      )
}
