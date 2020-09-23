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
    forkCount INT,
    FOREIGN KEY (owner) REFERENCES "user"(id)
);

CREATE TABLE IF NOT EXISTS star (
    repo VARCHAR,
    "user" VARCHAR,
    UNIQUE(repo, "user"),
    FOREIGN KEY (repo) REFERENCES repo(id),
    FOREIGN KEY ("user") REFERENCES "user"(id)
);