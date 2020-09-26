package graphql.resolvers.mutations

import graphql.resolvers.GUser
import graphql.schema.{R, User}
import graphql.storage
import graphql.storage.{Untagged, UserId, UserStorage}

object GMutateUser {

  case class MutateUserInput(
    id: Option[UserId],
    login: String,
    name: Option[String]
  )

  case class Input(
    in: MutateUserInput
  )

  case class MutateUserOutput(
    id: UserId,
    user: R[User]
  )

  def toStorage(i: MutateUserInput): UserStorage =
    UserStorage(
      i.id.getOrElse("".tag[UserStorage]),
      i.login,
      i.name
    )

  def mutate(in: MutateUserInput): R[MutateUserOutput] =
    for {
      id <- in.id.fold(
        storage.createUser(toStorage(in))
      )(i => storage.updateUser(toStorage(in)) as i)
    } yield
      MutateUserOutput(
        id,
        GUser.byId(id).run
      )
}
