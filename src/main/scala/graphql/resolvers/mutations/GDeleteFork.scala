package graphql.resolvers.mutations

import graphql.schema.{R, Repo}
import graphql.storage.RepoId

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

  def mutate(in: DeleteForkInput): R[DeleteForkOutput] = ???
}
