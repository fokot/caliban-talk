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
    fork: RepoId
  )

  implicit class Untagged[A](a: A) {
    def tag[T]: A @@ T = shapeless.tag[T][A](a)
  }


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

  implicit class ConnectionIOOps[A](c: ConnectionIO[Long]) {
    def single(e: => Exception): ConnectionIO[Unit] = c.flatMap {
      case 1L => Monad[ConnectionIO].pure(())
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

  def getUsers(ids: List[UserId]): R[List[UserStorage]] =
    transact(
      run (
        quote(
          query[UserStorage].filter(u => lift(ids).contains(u.id))
        )
      )
    )

  def getRepo(owner: String, name: String): R[RepoStorage] =
    transact(
      run (
        quote(
          query[RepoStorage].join(query[UserStorage]).on(
            (r, u) => r.owner == u.id && u.login == lift(owner) && r.name == lift(name)
          ).map(_._1)
        )
      ).single(new Exception(s"Not single repo for owner: $owner, name: $name"))
    )

  def getRepo(id: RepoId): R[RepoStorage] =
    transact(
      run (
        quote(
          query[RepoStorage].filter(_.id == lift(id))
        )
      ).single(new Exception(s"Not single repo for id: $id"))
    )

  def getRepos(owner: String): R[List[RepoStorage]] =
    transact(
      run (
        quote(
          query[RepoStorage].filter(r => r.owner == lift(owner))
        )
      )
    )

  def getAllRepos: R[List[RepoStorage]] =
    transact(
      run (
        quote(
          query[RepoStorage]
        )
      )
    )

  def getForksOf(owner: RepoId): R[List[RepoStorage]] =
    transact(
      run (
        quote(
          query[RepoStorage].join(query[ForkStorage]).on((r, f) => r.id == f.fork && f.origin == lift(owner)).map(_._1)
        )
      )
    )

  def createUser(user: UserStorage): RIO[TransactorService, UserId] =
    transact(
      run (
        quote(
          query[UserStorage].insert(lift(user)).returningGenerated(_.id)
        )
      )
    )

  def updateUser(user: UserStorage): RIO[TransactorService, Unit] =
    transact(
      run (
        quote(
          query[UserStorage].filter(_.id == lift(user.id)).update(lift(user))
        )
      ).single(new Exception(s"No user with id ${user.id}"))
    )

  def createRepo(repo: RepoStorage): RIO[TransactorService, RepoId] =
    transact(
      run (
        quote(
          query[RepoStorage].insert(lift(repo)).returningGenerated(_.id)
        )
      )
    )

  def updateRepo(repo: RepoStorage): RIO[TransactorService, Unit] =
    transact(
      run (
        quote(
          query[RepoStorage].filter(_.id == lift(repo.id)).update(lift(repo))
        )
      ).single(new Exception(s"No repo with id ${repo.id}"))
    )

  def createFork(fork: ForkStorage): RIO[TransactorService, Unit] =
    transact(
      run (
        quote(
          query[ForkStorage].insert(lift(fork))
        )
      )
    ).unit

  def deleteFork(fork: ForkStorage): RIO[TransactorService, Unit] =
    transact(
      run (
        quote(
          query[ForkStorage].filter(f => f.origin == lift(fork.origin) && f.fork == lift(fork.fork)).delete
        )
      ).single(new Exception(s"No fork with origin ${fork.origin} and fork ${fork.fork}"))
    )


}