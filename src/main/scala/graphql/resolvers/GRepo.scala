package graphql.resolvers

import graphql.schema.{R, Repo, RepoInput}
import graphql.storage.RepoStorage

object GRepo {

  def toGQL(r: RepoStorage): Repo = ???

  def get(in: RepoInput): R[Repo] = ???
}
