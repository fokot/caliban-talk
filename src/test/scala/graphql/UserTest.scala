package graphql

object UserTest extends GQLTestBase {

  override def uri: String = getClass.getClassLoader.getResource("user-test").getPath

}
