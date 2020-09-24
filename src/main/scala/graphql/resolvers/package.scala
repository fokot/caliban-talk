package graphql

import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.{ArgBuilder, GenericSchema}
import graphql.schema.{Env, Mutation, Query, Subscription}
import graphql.Auth.Auth
import graphql.resolvers.{GRepo, GUser}
import graphql.storage.{RepoId, UserId}
import zio.{Schedule, ZIO}
import zio.duration._
import zio.stream.ZStream


package object resolver {

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
      ZIO.access[Auth](_.get[Auth.Service].token.getOrElse("")),
      in => GUser.get(in),
      in => GRepo.get(in),
      ZIO.succeed(???)
    )

  private val mutation = Mutation(
    _ => ZIO.succeed(???),
    _ => ZIO.succeed(???),
    _ => ZIO.succeed(???),
    _ => ZIO.succeed(???),
  )

  private val subscription = Subscription(
    ZStream.succeed("Asdf").repeat(Schedule.spaced(1.second))
    //      ZStream.unfold(1)(i => Some((i.toString, i + 1)))
  )

  val api = graphQL(RootResolver(query, mutation, subscription))

}
