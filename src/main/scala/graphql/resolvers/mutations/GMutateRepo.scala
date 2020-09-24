package graphql.resolvers.mutations

import graphql.schema.{R, Repo}
import graphql.storage.{RepoId, UserId}

object GMutateRepo {

  case class MutateRepoInput(
    id: RepoId,
    name: String,
    ownerId: UserId
  )

  case class Input(
    in: MutateRepoInput
  )

  case class MutateRepoOutput(
    success: Boolean,
    repo: R[Repo]
  )

  def mutate(in: MutateRepoInput): R[MutateRepoOutput] = ???

}
