DROP TABLE IF EXISTS user_, article, "comment";

CREATE TABLE user_
(
  id bigserial                   NOT NULL,
  username character varying(32) NOT NULL,
  CONSTRAINT user__pkey PRIMARY KEY (id)
);

CREATE TABLE "comment"
(
  id        bigserial NOT NULL,
  body      text      NOT NULL,
  userid    bigint    NOT NULL,
  articleid bigint    NOT NULL,
  CONSTRAINT comment_pkey PRIMARY KEY (id)
);

CREATE TABLE article
(
  id     bigserial NOT NULL,
  title  text      NOT NULL,
  teaser text      NOT NULL,
  body   text      NOT NULL,
  userid bigint    NOT NULL,
  CONSTRAINT article_pkey PRIMARY KEY (id)
);
