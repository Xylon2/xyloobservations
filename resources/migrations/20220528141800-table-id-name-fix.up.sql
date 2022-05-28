alter table image
rename column id to image_id;

--;;

alter table tag
rename column id to tag_id;

--;;

alter table imagetag
rename column id to imagetag_id;
