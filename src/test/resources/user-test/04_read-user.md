[Auth](../auth/viewer.json)

### Query:
```
query readUser($login: String!) {
  user(login: $login) {
    id
    login
    name
  }
}
```

### Variables:
```json
{
  "login": "bob"
}
```

### Result:
```json
{
  "data": {
    "user": {
      "id": "<<<user-id",
      "login": "bob",
      "name": "bob johnson"
    }
  }
}
```
