# Example Caliban app for the talk

Slides can be found [here](https://docs.google.com/presentation/d/1TZLCPy2VggDhar_l5yO3LpkAHMzm3aLUZ1tUyaSoJqM)

In this application you can create `users`, `repos` and `forks` and also import them from `Github`.

## Running application

Application is in `graphql` package.
To start server
* have docker installed
* run `graphql.CalibanApp`
* open [http://localhost:8088/graphiql](http://localhost:8088/graphiql)

If you want to run it against local postgres
* go to `CalibanApp` and setup `customLayer`
* setup db in `application.conf` or overwrite it `env.conf`

If you enable authorization in `graphql.package.resolver` and you can test it by inputting token
to top right corner in graphiql e.g. `{"name": "String", "roles": ["VIEWER"]}` 

## Libraries used:
* [caliban](https://ghostdogpr.github.io/caliban) - graphql server
* [caliban-client](https://ghostdogpr.github.io/caliban/docs/client.html) - graphql client
* [zio](https://zio.dev) - effect
* [http4s](https://http4s.org) - web server
* [circe](https://circe.github.io/circe) - json
* [doobie](https://tpolecat.github.io/doobie) and [quill](https://getquill.io) - database
* [sttp](https://sttp.softwaremill.com) - http client
* [shapeless](https://github.com/milessabin/shapeless) - just for type tags
* [testcontainers-scala](https://github.com/testcontainers/testcontainers-scala) - postgresql in docker container in tests
* [flyway](https://flywaydb.org) - migrations

## GraphQL tests
Tests are written in markdown files. Single file represent single request. One directory is a test suite.
Requests/files are executes sequentially ordered by file names. To create test you need only extend 
`GQLTestBase` as it is done in [UserTest](src/test/scala/graphql/UserTest.scala). You can store values to 
cache between requests by using `<<<` and read them by using `<<<` as you can see in 
[01_create-user.md](src/test/resources/user-test/01_create-user.md). Stored value might be any json, not
just string. String is used only as a placeholder.

## Example package

Example package contains examples shown in the talk which were not part of our application:
* [**GithubQuery**](src/main/scala/example/GithubQuery.scala) - standalone example showing how to call `Github` graphql api with the help of `caliban-client`
* [**MagnoliaExample**](src/main/scala/example/MagnoliaExample.scala) - `Magnolia` simple use case - used in `Caliban` for typeclass derivations
* [**SlideExamples**](src/main/scala/example/SlideExamples.scala) - examples which were on the slides
* [**ZIOFirstExample**](src/main/scala/example/ZIOFirstExample.scala) - simple `ZIO` app
* [**ZIOTypeInference**](src/main/scala/example/ZIOTypeInference.scala) - example how type inference works in `ZIO`

## GithubApi.scala

Client was generated via
```
calibanGenClient src/main/resources/schema.docs.graphql src/main/scala/graphql/GithubApi.scala
```
