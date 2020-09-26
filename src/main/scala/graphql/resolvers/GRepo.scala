package graphql.resolvers

import graphql.schema.{R, Repo, RepoInput}
import graphql.storage
import graphql.storage.{RepoId, RepoStorage, UserId}
import zio.ZIO

object GRepo {

  def toGQL(r: RepoStorage): R[Repo] =
    storage.getForksOf(r.id).memoize.map(forks =>
      Repo(
        r.id,
        r.name,
        r.owner + "/" + r.name,
        GUser.byId(r.owner),
        forks.map(_.size),
        forks.flatMap(ZIO.foreach(_)(toGQL))
      )
    )

  def get(in: RepoInput): R[Repo] =
    storage.getRepo(in.owner, in.name).flatMap(toGQL)

  def forUser(owner: UserId): R[List[Repo]] =
    storage.getRepos(owner).flatMap(ZIO.foreach(_)(toGQL))

  val all: R[List[Repo]] =
    storage.getAllRepos.flatMap(ZIO.foreach(_)(toGQL))

  def byId(id: RepoId): R[Repo] =
    storage.getRepo(id).flatMap(toGQL)

}
