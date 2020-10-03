[Auth](../auth/editor.json)

### Query:
```
mutation updateUser($in: MutateUserInput!){
  mutateUser(in: $in) {
    user {
      login
    }
  }
}
```

### Variables:
```json
{
  "in": {
    "id": "<<<user-id",
    "login": "bob",
    "name": "bob johnson"
  }
}
```

### Result:
```json
{
  "data": {
    "mutateUser": {
      "user": {
        "login": "bob"
      }
    }
  }
}
```
