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
    image_id
from
    imagetag
right join image
    on imagetag.image_ref = image.image_id
where imagetag.imagetag_id is null;

-- :name tags-with-images :? :*
-- :doc get tags that have images. will contain duplicates
select
    tag_id,
    tag_name,
    description
from
    imagetag
inner join tag
    on imagetag.tag_ref = tag.tag_id

-- :name images-by-tag :? :*
-- :doc get ids of all the images which have a certain tag
select image_ref from imagetag
where tag_ref = :tag_ref

-- :name fetch-image :? :1
-- :doc fetch image data
select imagedata from image
where image_id = :image_id;
