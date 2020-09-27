package graphql.resolvers.mutations

import graphql.schema.R
import graphql.{github, storage}

object GGithubImport {

  case class GithubImportInput(
    query: String,
  )

  case class Input(
    in: GithubImportInput
  )

  case class GithubImportOutput(
    success: Boolean,
  )

  def mutate(in: GithubImportInput): R[GithubImportOutput] =
    github.importFromGithub(in.query)
      .flatMap((storage.saveImport _).tupled)
      .as(GithubImportOutput(true))

}
