alter table tag
alter column advanced
drop default;

--;;

alter table tag
alter column advanced type boolean
using false;

--;;

alter table tag
alter column advanced
set default false;
