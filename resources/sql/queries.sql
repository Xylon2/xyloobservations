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
insert into tag (tag_name, description)
values (:tagname, :description)

-- :name upload-image! :! :n
-- :doc upload the image
insert into image (imagedata, mimetype, caption)
values (:imagedata, :mimetype, :caption)

-- :name orphan-images :? :*
-- :doc get images which do not have a tag associated
select
    image_id,
    caption
from
    imagetag
right join image
    on image_ref = image_id
where imagetag_id is null;

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
select
    image_id,
    caption
from
    imagetag
inner join image
    on image_ref = image_id
where tag_ref = :tag_ref

-- :name fetch-image :? :1
-- :doc fetch image data
select imagedata, mimetype from image
where image_id = :image_id;

-- :name tag_names_of_image :? :*
-- :doc get names and ids of all tags attached to a given image
select 
    tag_id,
    tag_name
from 
    imagetag
inner join tag
    on tag_ref = tag_id
where image_ref = :image_id;

-- :name all_tags :? :*
-- :doc get names and ids of all tags
select tag_id, tag_name 
from tag;

-- :name tag-image! :! :n
-- :doc add a tag to an image
insert into imagetag (tag_ref, image_ref)
values (:tag_id, :image_id);

-- :name get-caption :? :1
-- :doc get the caption for an image
select caption from image
where image_id = :image_id;

-- :name update-caption! :! :n
-- :doc update the caption for an image
update image
set caption = :newcaption
where image_id = :image_id;
