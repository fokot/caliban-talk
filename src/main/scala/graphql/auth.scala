package graphql

import io.circe.{Codec, Decoder}
import zio.{Has, RIO, ZIO}
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

object auth {

  object AuthException extends Exception("Permission denied")

  sealed trait Role

  object Role {
    case object EDITOR extends Role
    case object VIEWER extends Role
  }

  case class AuthUser(name: String, roles: List[Role])

  implicit val codecAuthUser: Codec[AuthUser] = deriveCodec[AuthUser]
  implicit val codecRole: Codec[Role] = deriveEnumerationCodec[Role]

  type Authorized = RIO[Auth, AuthUser]

  val isAuthenticated: Authorized = ZIO.accessM[Auth](r => ZIO.succeed(r.get.user).someOrFail(new Exception("Invalid token")))

  /**
   * Will succeed if user has at least one of specified roles
   */
  def hasRole(r: Role*): Authorized =
    isAuthenticated.filterOrFail(_.roles.intersect(r).nonEmpty)(AuthException)

  val isEditor: Authorized = hasRole(Role.EDITOR)
  val isViewer: Authorized = hasRole(Role.VIEWER)


  type Auth = Has[auth.Service]

  trait Service {
    def token: Option[String]
    def user: Option[AuthUser]
  }

  def fromToken(_token: Option[String]): Service =
    new Service {
      override def token: Option[String] = _token
      override val user: Option[AuthUser] = token.flatMap(io.circe.parser.parse(_).flatMap(_.as[AuthUser]).toOption)
    }
}