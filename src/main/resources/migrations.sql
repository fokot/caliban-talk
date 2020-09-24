CREATE TABLE IF NOT EXISTS "user" (
    id VARCHAR PRIMARY KEY,
    login VARCHAR UNIQUE NOT NULL,
    name VARCHAR
);

CREATE TABLE IF NOT EXISTS repo (
    id VARCHAR PRIMARY KEY ,
    name VARCHAR NOT NULL,
    owner VARCHAR NOT NULL,
    UNIQUE (name, owner),
    FOREIGN KEY (owner) REFERENCES "user"(id)
);

CREATE TABLE IF NOT EXISTS fork (
    origin VARCHAR NOT NULL,
    fork VARCHAR NOT NULL,
    UNIQUE(origin, fork),
    FOREIGN KEY (origin) REFERENCES repo(id),
    FOREIGN KEY (fork) REFERENCES repo(id)
);