alter table image
rename column image_id to id;

--;;

alter table tag
rename column tag_id to id;

--;;

alter table imagetag
rename column imagetag_id to id;
