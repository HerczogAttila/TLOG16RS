alter table task drop foreign key fk_task_work_day_id;
drop index ix_task_work_day_id on task;

alter table work_day drop foreign key fk_work_day_work_month_id;
drop index ix_work_day_work_month_id on work_day;

alter table work_month drop foreign key fk_work_month_time_logger_id;
drop index ix_work_month_time_logger_id on work_month;

drop table if exists task;

drop table if exists time_logger;

drop table if exists work_day;

drop table if exists work_month;

