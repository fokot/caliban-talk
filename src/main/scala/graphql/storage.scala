package graphql

import cats.{Monad, MonadError}
import doobie.ConnectionIO
import doobie.implicits._
import doobie.quill.DoobieContext
import zio.interop.catz.taskConcurrentInstance
import graphql.Auth.{Auth, AuthUser}
import graphql.Transactor.TransactorService
import io.getquill
import io.getquill.SnakeCase
import shapeless.tag
import shapeless.tag.@@
import zio.{RIO, ZIO}

object storage {

  type UserId = String @@ UserStorage
  type RepoId = String @@ RepoStorage

  case class UserStorage(
    id: UserId,
    login: String,
    name: Option[String]
  )

  case class RepoStorage(
    id: RepoId,
    name: String,
    owner: UserId
  )

  case class ForkStorage(
    origin: RepoId,
    name: RepoId
  )

  private def taggedTypeEncoder[A, B] = getquill.MappedEncoding[B @@ A, B](identity)
  private def taggedTypeDecoder[A, B] = getquill.MappedEncoding[A, A @@ B](tag[B][A])

  implicit val userIdEncoder = taggedTypeEncoder[UserStorage, String]
  implicit val userIdDecoder = taggedTypeDecoder[String, UserStorage]

  implicit val repoIdEncoder = taggedTypeEncoder[RepoStorage, String]
  implicit val repoIdDecoder = taggedTypeDecoder[String, RepoStorage]

  // remove storage suffix from table names and quote them because `user` is keyword in postgres
  object NamingStrategy extends SnakeCase {
    override def table(s: String): String =
      "\"" ++ super.default(s).stripSuffix("_storage") ++ "\""
  }

  val dc = new DoobieContext.Postgres(NamingStrategy)
  import dc._

  implicit class ConnectionIOListOps[A](c: ConnectionIO[List[A]]) {
    def single(e: => Exception): ConnectionIO[A] = c.flatMap {
      case List(a) => Monad[ConnectionIO].pure(a)
      case _ => MonadError[ConnectionIO, Throwable].raiseError(e)
    }
  }

  def transact[A](c: ConnectionIO[A]): RIO[TransactorService, A] =
    ZIO.accessM[TransactorService](r => c.transact(r.get))

  // transact and provide current authorised user
  def transactUser[A](c: AuthUser => ConnectionIO[A]): RIO[TransactorService with Auth, A] =
    Auth.isAuthenticated.flatMap(u => transact(c(u)))

  type R[A] = RIO[TransactorService, A]

  def getUserByLogin(login: String): R[UserStorage] =
    transact(
      run (
        quote(
          query[UserStorage].filter(_.login == lift(login))
        )
      ).single(new Exception(s"Not single user for login: $login"))
    )




}