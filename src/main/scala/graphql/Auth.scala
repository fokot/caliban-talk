package graphql

import zio.{Has, RIO, ZIO}
import io.circe.generic.auto._

object Auth {

  object AuthException extends Exception("Permission denied")

  sealed trait Role

  object Role {
    case object EDITOR extends Role
    case object VIEWER extends Role
  }

  case class User(name: String, roles: List[Role])

  type Authorized = RIO[Auth, User]

  val isAuthenticated: Authorized = ZIO.accessM[Auth](r => ZIO.getOrFail(r.get.user))

  /**
   * Will succeed if user has at least one of specified roles
   */
  def hasRole(r: Role*): Authorized =
    isAuthenticated.filterOrFail(_.roles.intersect(r).nonEmpty)(AuthException)

  val isEditor: Authorized = hasRole(Role.EDITOR)
  val isViewer: Authorized = hasRole(Role.VIEWER)


  type Auth = Has[Auth.Service]

  trait Service {
    def token: Option[String]
    def user: Option[User]
  }

  def fromToken(_token: Option[String]): Service =
    new Service {
      override def token: Option[String] = _token
      override val user: Option[User] = token.flatMap(io.circe.parser.parse(_).flatMap(_.as[User]).toOption)
    }
}