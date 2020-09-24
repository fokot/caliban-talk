CREATE TABLE IF NOT EXISTS "user" (
    id VARCHAR PRIMARY KEY,
    login VARCHAR,
    name VARCHAR,
    UNIQUE(login)
);

CREATE TABLE IF NOT EXISTS repo (
    id VARCHAR PRIMARY KEY ,
    name VARCHAR,
    owner VARCHAR,
    FOREIGN KEY (owner) REFERENCES "user"(id)
);

CREATE TABLE IF NOT EXISTS fork (
    origin VARCHAR,
    fork VARCHAR,
    UNIQUE(origin, fork),
    FOREIGN KEY (origin) REFERENCES repo(id),
    FOREIGN KEY (fork) REFERENCES repo(id)
);