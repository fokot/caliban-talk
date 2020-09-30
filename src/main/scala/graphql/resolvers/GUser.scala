package graphql.resolvers

import graphql.resolver.RequestId
import graphql.schema.{Env, Q, R, User, UserInput}
import graphql.storage
import graphql.storage.{UserId, UserStorage}
import zio.Chunk
import zio.query.{DataSource, ZQuery}

object GUser {

  def toGQL(r: UserStorage): User = User(
    r.id,
    r.login,
    r.name,
    GRepo.forUser(r.id)
  )

  def get(in: UserInput): R[User] =
    storage.getUserByLogin(in.login).map(toGQL)


  val dataSource: DataSource[Env, RequestId[UserId, UserStorage]] =
    DataSource.fromFunctionBatchedWithM("user-by-id")(
      requests => storage.getUsers(requests.map(_.id).toList).map(Chunk.fromIterable),
      (u: UserStorage) =>
        RequestId(u.id)
    )


  def byId(id: UserId): Q[User] =
    ZQuery.fromRequest(RequestId[UserId, UserStorage](id))(dataSource).map(toGQL)

}
