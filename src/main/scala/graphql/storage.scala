package graphql

import shapeless.tag.@@

object storage {

  type UserId = String @@ SUser
  type RepoId = String @@ SRepo

  case class SUser(
    id: UserId,
    login: String,
    name: String
  )

  case class SRepo(
    id: RepoId,
    name: String,
    nameWithOwner: String,
    owner: UserId
  )

  case class SFork(
    origin: RepoId,
    name: RepoId
  )

}
