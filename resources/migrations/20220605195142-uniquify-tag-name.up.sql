alter table tag
add constraint dedup_name unique (tag_name);
