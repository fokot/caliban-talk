package example

import magnolia._
import scala.language.experimental.macros

/**
 * Magnolia is used in caliban to derive typeclasses from schema case classes
 * Example of typeclass derivation library Magnolia https://propensive.com/opensource/magnolia
 * Example taken from https://propensive.com/opensource/magnolia/tutorial
 */
object MagnoliaExample extends App {

  trait Show[T] {
    def show(value: T): String
  }

  object MyDerivation {
    type Typeclass[T] = Show[T]

    def combine[T](caseClass: CaseClass[Show, T]): Show[T] = new Show[T] {
      def show(t: T): String = {
        val paramStrings = caseClass.parameters.map { p =>
          p.label+"="+p.typeclass.show(p.dereference(t))
        }

        caseClass.typeName.short+paramStrings.mkString("(", ",", ")")
      }
    }

    def dispatch[T](sealedTrait: SealedTrait[Show, T]): Show[T] = new Show[T] {
      def show(t: T): String = sealedTrait.dispatch(t) { subtype =>
        subtype.typeclass.show(subtype.cast(t))
      }
    }

    implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
  }

  implicit val showString: Show[String] = new Show[String] {
    override def show(value: String): String = value
  }

  case class A(f: String)

  val x = MyDerivation.gen[A]

  println(x.show(A("asdf")))

}
