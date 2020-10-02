# Example Caliban app for a talk

Presentation can be found [here](https://docs.google.com/presentation/d/1TZLCPy2VggDhar_l5yO3LpkAHMzm3aLUZ1tUyaSoJqM)

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

## Running application

Application is in `graphql` package.
To start server
* have a postgress instance and run migrations from `src/main/resources/migration`
* setup db in `application.conf` or overwrite it `env.conf`
* run `graphql.CalibanApp`
* open [http://localhost:8088/graphiql](http://localhost:8088/graphiql)

If you enable authorization in `graphql.package.resolver` and you can test it by inputting token
to top right corner in graphiql e.g. `{"name": "String", "roles": ["VIEWER"]}` 

## Example package

Example package contains examples shown in the talk which were not part of our application:
* **GithubQuery** - standalone example showing how to call `Github` graphql api with the help of `caliban-client`
* **MagnoliaExample** - `Magnolia` simple use case - used in `Caliban` for typeclass derivations
* **SlideExamples** - examples which were on the slides
* **ZIOFirstExample** - simple `ZIO` app
* **ZIOTypeInference** - example how type inference works in `ZIO`

##

Client was generated via
```
calibanGenClient src/main/resources/schema.docs.graphql src/main/scala/graphql/GithubApi.scala
```
