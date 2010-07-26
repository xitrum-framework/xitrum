DROP TABLE IF EXISTS user_, article, "comment";

CREATE TABLE user_
(
  id serial                      NOT NULL,
  username character varying(32) NOT NULL,
  CONSTRAINT user__pkey PRIMARY KEY (id)
);

CREATE TABLE article
(
  id        serial                      NOT NULL,
  title     text                        NOT NULL,
  teaser    text                        NOT NULL,
  body      text                        NOT NULL,
  sticky    boolean                     NOT NULL,
  hits      int                         NOT NULL,
  createdat timestamp without time zone NOT NULL,
  updatedat timestamp without time zone NOT NULL,
  userid    int                         NOT NULL,
  CONSTRAINT article_pkey PRIMARY KEY (id)
);

CREATE TABLE "comment"
(
  id        serial                      NOT NULL,
  body      text                        NOT NULL,
  createdat timestamp without time zone NOT NULL,
  updatedat timestamp without time zone NOT NULL,
  userid    int                         NOT NULL,
  articleid int                         NOT NULL,
  CONSTRAINT comment_pkey PRIMARY KEY (id)
);
