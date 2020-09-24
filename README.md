# Example Caliban app for a talk

//FIXME append links and libraries

Libraries used:
* [caliban]() - graphql server
* [caliban-client]() - graphql client
* [zio]() - effect
* [http4s]() - web server
* [circe]() - json
* [doobie]() and [quill]() - database
* [sttp]() - http client
* [shapeless]() - just for type tags


Client was generated via
```
calibanGenClient src/main/resources/schema.docs.graphql src/main/Client.scala
```
