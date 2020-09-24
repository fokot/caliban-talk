package graphql.resolvers

import graphql.schema.{R, User, UserInput}
import graphql.storage
import graphql.storage.UserStorage
import zio.ZIO

object GUser {

  def toGQL(r: UserStorage): User = User(
    r.id,
    r.login,
    r.name,
    ZIO.succeed(Nil)
  )

  def get(in: UserInput): R[User] =
    storage.getUserByLogin(in.login).map(toGQL)

}
