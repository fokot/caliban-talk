[Auth](../auth/editor.json)

### Query:
```
mutation createUser($in: MutateUserInput!){
  mutateUser(in: $in) {
    id
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
    "login": "bob",
    "name": "bob novak"
  }
}
```

### Result:
```json
{
  "data": {
    "mutateUser": {
      "id": ">>>user-id",
      "user": {
        "login": "bob"
      }
    }
  }
}
```