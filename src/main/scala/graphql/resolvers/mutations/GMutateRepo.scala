package graphql.resolvers.mutations

import graphql.resolvers.GRepo
import graphql.schema.{R, Repo}
import graphql.storage
import graphql.storage.{RepoId, RepoStorage, Untagged, UserId}

object GMutateRepo {

  case class MutateRepoInput(
    id: Option[RepoId],
    name: String,
    ownerId: UserId
  )

  case class Input(
    in: MutateRepoInput
  )

  case class MutateRepoOutput(
    id: RepoId,
    repo: R[Repo]
  )

  def toStorage(i: MutateRepoInput): RepoStorage =
    RepoStorage(
      i.id.getOrElse("".tag[RepoStorage]),
      i.name,
      i.ownerId
    )

  def mutate(in: MutateRepoInput): R[MutateRepoOutput] =
    for {
      id <- in.id.fold(
        storage.createRepo(toStorage(in))
      )(i => storage.updateRepo(toStorage(in)) as i)
    } yield
      MutateRepoOutput(
        id,
        GRepo.byId(id)
      )


}
