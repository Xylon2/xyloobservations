alter table image
add imagedata bytea,
add mimetype text not null default 'image/jpeg';
