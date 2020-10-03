[Auth](../auth/editor.json)

### Query:
```
query readUser($login: String!) {
  user(login: $login) {
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
      "login": "bob",
      "name": "bob novak"
    }
  }
}
```
