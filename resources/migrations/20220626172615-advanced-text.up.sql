create type adstates as enum ('false', 'date', 'place');

--;;

alter table tag
alter column advanced
drop default;

--;;

alter table tag
alter column advanced type adstates
using 'false';

--;;

alter table tag
alter column advanced
set default 'false';
