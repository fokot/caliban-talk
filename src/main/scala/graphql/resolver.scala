package graphql

import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.{ArgBuilder, GenericSchema}
import graphql.schema.{Env, Mutation, Query, Subscription}
import graphql.Auth.Auth
import graphql.storage.{RepoId, UserId}
import zio.{Schedule, ZIO}
import zio.clock.Clock
import zio.duration._
import zio.stream.ZStream


object resolver {

  // our GraphQL API
  private val schema: GenericSchema[Env] = new GenericSchema[Env] {}
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
      _ => ZIO.succeed(???),
      _ => ZIO.succeed(???),
      ZIO.succeed(???)
    )

  private val mutation = Mutation(
    ZIO.succeed("aa")
  )

  private val subscription = Subscription(
    ZStream.succeed("Asdf").repeat(Schedule.spaced(1.second))
//      ZStream.unfold(1)(i => Some((i.toString, i + 1)))
  )

  val api = graphQL(RootResolver(query, mutation, subscription))

}
