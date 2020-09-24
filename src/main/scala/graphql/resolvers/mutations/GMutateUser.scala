package graphql.resolvers.mutations

import graphql.schema.{R, User}
import graphql.storage.UserId

object GMutateUser {

  case class MutateUserInput(
    id: Option[UserId],
    login: String,
    name: String
  )

  case class Input(
    in: MutateUserInput
  )

  case class MutateUserOutput(
    success: Boolean,
    user: R[User]
  )

  def mutate(in: MutateUserInput): R[MutateUserOutput] = ???

}
