-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
insert into users
(login, password)
values (:login, :password)

-- :name get-user-for-auth* :? :1
-- :doc selects a user for authentication
select * from users
where login = :login

-- :name add-tag! :! :n
-- :doc add a tag
insert into tag
(tag_name, description)
values (:tagname, :description)

-- :name upload-image! :! :n
-- :doc upload the image
insert into image
(imagedata)
values (:imagedata)

-- :name orphan-images :? :*
-- :doc get images which do not have a tag associated
select
    imagetag_id,
    tag_ref,
    image_ref,
    image_id
from
    imagetag
right join image
    on image_ref = image_id
where imagetag_id is null;

-- :name fetch-image :? :1
-- :doc fetch image data
select imagedata from image
where image_id = :image_id;
