alter table image
add crop_data jsonb not null default '{"hpercent":100,"vpercent":100,"hoffset":0,"voffset":0}'::jsonb;
