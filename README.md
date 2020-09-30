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

##

Client was generated via
```
calibanGenClient src/main/resources/schema.docs.graphql src/main/scala/graphql/GithubApi.scala
```
