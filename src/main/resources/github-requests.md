# Example github requests

Execute them on [https://developer.github.com/v4/explorer](https://developer.github.com/v4/explorer)

## Search
```
query { 
  search(query: "caliban", type: REPOSITORY, first: 3) {
    repositoryCount
    nodes {
      __typename
      ... on Repository {
        owner {
          login
        }
        name
        primaryLanguage {
          name
        }
     	createdAt
  	  }
    }
  }
}
```

## Organization
```
query {
  organization(login:"scala") {
  	repositories(first:100) {
      nodes {
        name
        primaryLanguage {
          name
        }
        forkCount
        stargazers(first:10) {
          totalCount
          nodes {
            login
          }
        }
      }
    }
  }
}

```