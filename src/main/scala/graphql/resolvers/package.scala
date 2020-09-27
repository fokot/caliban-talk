package graphql

import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.{ArgBuilder, GenericSchema}
import graphql.schema.{Env, Mutation, Query, Subscription}
import graphql.auth.Auth
import graphql.resolvers.mutations.{GCreateFork, GDeleteFork, GGithubImport, GMutateRepo, GMutateUser}
import graphql.resolvers.{GRepo, GUser}
import graphql.storage.{RepoId, UserId}
import zio.{Schedule, ZIO, random}
import zio.duration._
import zio.query.Request
import zio.stream.ZStream


package object resolver {

  case class RequestId[ID, A](id: ID) extends Request[Nothing, A]

  // our GraphQL API
  private val schema: GenericSchema[Env] = new GenericSchema[Env] {
    // otherwise `Input type names` will have `Input` after case class name
    override def customizeInputTypeName(name: String): String = name
  }
  import schema._

  private def tagSchema[A, B <: A](implicit s: schema.Typeclass[A]): schema.Typeclass[B] = s.asInstanceOf[schema.Typeclass[B]]
  private def tagArgBuilder[A, B <: A](implicit s: ArgBuilder[A]): ArgBuilder[B] = s.asInstanceOf[ArgBuilder[B]]

  implicit private val schemaUserId = tagSchema[String, UserId]
  implicit private val argBuilderUserId = tagArgBuilder[String, UserId]

  implicit private val schemaRepoId = tagSchema[String, RepoId]
  implicit private val argBuilderRepoId = tagArgBuilder[String, RepoId]

  private val query =
    Query(
      ZIO.access[Auth](_.get[auth.Service].token.getOrElse("")),
      in => GUser.get(in),
      in => GRepo.get(in),
      GRepo.all,
    )

  private val mutation = Mutation(
    in => GMutateUser.mutate(in.in),
    in => GMutateRepo.mutate(in.in),
    in => GCreateFork.mutate(in.in),
    in => GDeleteFork.mutate(in.in),
    in => GGithubImport.mutate(in.in),
  )

  private val subscription = Subscription(
    ZStream.fromEffect(
      for {
        repos <- storage.getAllRepos
        r <- random.nextIntBounded(repos.length)
        repo <- GRepo.toGQL(repos(r))
      } yield repo
    ).repeat(Schedule.spaced(1.second))
  )

  val api = graphQL(RootResolver(query, mutation, subscription))

}
