create table users (
  login text primary key,
  password text not null,
  created_at timestamp not null default now()
);
