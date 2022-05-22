create table image (
    id serial not null primary key,
    imagedata bytea
);

--;;

create table tag (
    id serial not null primary key,
    tag_name    varchar(100),
    description varchar(300)
);

--;;

create table imagetag (
    id serial not null primary key,
    image_ref integer references image (id) on delete cascade,
    tag_ref   integer references tag   (id) on delete cascade
);
