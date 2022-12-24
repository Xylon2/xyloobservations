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
insert into tag (tag_name, description, advanced)
values (:tagname, :description, :advanced::adstates)

-- :name modify-tag! :! :n
-- :doc modify a tag
update tag set
  tag_name = :tag_name,
  description = :description,
  advanced = :advanced::adstates
where tag_id = :tag_id::integer

-- :name reference-image! :! :1
-- :doc add an image which is in s3
insert into image (object_ref, caption)
values (:object_ref, :caption)
returning image_id

-- :name orphan-images :? :*
-- :doc get images which do not have a tag associated
select
    image_id,
    object_ref,
    caption,
    imagemeta
from
    imagetag
right join image
    on image_ref = image_id
where imagetag_id is null
and progress = 'complete';

-- :name images-by-tag :? :*
-- :doc get ids of all the images which have a certain tag
select
    image_id,
    caption
from
    imagetag
inner join image
    on image_ref = image_id
where tag_ref = :tag_ref::integer

-- :name tag_names_of_image :? :*
-- :doc get names and ids of all tags attached to a given image
select 
    tag_id,
    tag_name
from 
    imagetag
inner join tag
    on tag_ref = tag_id
where image_ref = :image_id::integer;

-- :name all_tags :? :*
-- :doc get names and ids of all tags
select tag_id, tag_name
from tag
order by advanced, tag_name;

-- :name tag-image! :! :n
-- :doc add tags to an image
insert into imagetag (tag_ref, image_ref)
values 
/*~
(let [taglist  (map parse-long (:taglist params))
      image_id (:image_id params)]
  (clojure.string/join ", " (map #(str "(" % ", " image_id ")") taglist)))
~*/

-- :name untag-image! :! :n
delete from imagetag
where image_ref = :image_id::integer
and tag_ref = :tag::integer;

-- :name caption-and-object :? :*
-- :doc get the caption and object_ref for an image
select
    image_id,
    object_ref,
    caption,
    imagemeta
from image
where image_id = :image_id::integer;

-- :name update-caption! :! :n
-- :doc update the caption for an image
update image
set caption = :newcaption
where image_id = :image_id::integer;

-- :name find-imagetag :? :1
-- :doc is there an imagetag entry linking a specific tag and image
select count(*)
from imagetag
where image_ref = :image_id::integer
and tag_ref = :tag_id::integer;

-- :name images-with-tags :? :*
-- :doc gets all image ids and captions that have any tag
select
    image_id,
    object_ref,
    caption,
    imagemeta
from
    imagetag
inner join image
    on image_ref = image_id
where progress = 'complete'
order by image_id desc;

-- :name images-multi-tags :? :*
-- :doc find images that have all of the provided tags. accepts vector of tags
select
    image_id,
    object_ref,
    caption,
    imagemeta
from
    image
where image_id in
(
/*~
(let [taglist (->> params :tags (map parse-long))]
  (cons
    (str "select image_ref from imagetag where tag_ref = " (last taglist))
    (map #(str "intersect select image_ref from imagetag where tag_ref = " %) (pop taglist))))
~*/
)
and progress = 'complete'
order by image_id desc;

-- :name random-images :? :*
-- :doc return the specified number of random images that have tags
with distinctimages as (
    select distinct
        image_id,
        object_ref,
        caption,
        imagemeta,
        progress
    from
        imagetag
    inner join image
        on image_id = image_ref
)
select
    image_id,
    object_ref,
    caption,
    imagemeta
from
    distinctimages
where progress = 'complete'
order by random() limit :numimages::integer

-- :name names-for-tags :? :*
-- :doc given a list of tag ids, return ids and names
select tag_id, tag_name, description
from tag
where tag_id = any(array[:v*:tags]::integer[])

-- :name tag-info :? :1
-- :doc info about one tag
select :i*:cols
from tag
where tag_id = :tag_id::integer;

-- :name all-tags-with-images :? :*
-- :doc return all tags which have an image
select distinct
    tag_id, tag_name, advanced
from
    imagetag
inner join tag
    on tag_id = tag_ref
order by advanced, tag_name;

-- :name delete-image! :! :n
-- :doc delete an image
delete from image
where image_id = :image_id::integer;

-- :name delete-tag! :! :n
-- :doc delete a tag
delete from tag
where tag_id = :tag_id::integer;

-- :name update-progress! :! :n
-- :doc updates the progress on an image
update image
set progress = :progress
where image_id = :image_id::integer;

-- :name get-progress :? :1
-- :doc get the processing progress for an image
select progress from image
where image_id = :image_id::integer;

-- :name save-meta! :! :n
-- :doc adds metadata to an image
update image
set imagemeta = :imagemeta::jsonb
where image_id = :image_id::integer;
