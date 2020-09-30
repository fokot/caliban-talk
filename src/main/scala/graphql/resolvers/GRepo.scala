package graphql.resolvers

import graphql.schema.{R, Repo, RepoInput}
import graphql.storage
import graphql.storage.{RepoId, RepoStorage, UserId}
import zio.ZIO

object GRepo {

  // function made so I can `tap` on it
  def getForks(id: RepoId) =
    storage.getForksOf(id).tap(_ => zio.console.putStrLn("get forks"))

  def toGQL(r: RepoStorage): R[Repo] =
    getForks(r.id).memoize.map(forks =>
      Repo(
        r.id,
        r.name,
        GUser.byId(r.owner).map(_.login + "/" + r.name),
        GUser.byId(r.owner),
//        getForks(r.id).map(_.size),
//        getForks(r.id).flatMap(ZIO.foreach(_)(toGQL)),
        forks.map(_.size),
        forks.flatMap(ZIO.foreach(_)(toGQL)),
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
