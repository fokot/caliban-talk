package example

import zio.{ExitCode, Has, Task, URIO}
import zio.console.Console

object ZIOTypeInference {

  /**
   * ZIO[-R, +E, +A]
   * E and A parameter are covariant
   * It means it will infer the least common super type
   * Error works the same way as Value
   * It works the same as Option[+A]
   */
  object value {

    trait A
    trait B extends A
    trait C extends B
    trait D extends B

    case class X(i: Int)
    val a: Task[A] = ???
    val b: Task[B] = ???
    val c: Task[C] = ???
    val d: Task[D] = ???

    val ab: Task[A] = if(true) a else b
    val cd: Task[B] = if(true) c else d
  }

  object environment {

    trait CusService {
      def getCustomer: String
    }
    trait UsService {
      def getUser: String
    }

    // has creates key into heterogenous Map
    type CustomerService = Has[CusService]
    val cs: CustomerService = ???

    type UserService = Has[UsService]
    val us: UserService = ???

    // easily combined on type level as well as on value level
    type AppEnv = CustomerService with UserService
    val env: AppEnv = cs ++ us

  }

}
