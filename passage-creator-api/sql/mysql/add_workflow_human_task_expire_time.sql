-- 为已存在的 workflow_human_task 表补充人工任务过期时间。
-- 如果是全新库，add_workflow_tables.sql 已包含该字段；老库需要单独执行本脚本。

alter table workflow_human_task
    add column expire_time datetime null comment '人工任务过期时间' after create_time;

create index idx_human_task_status_expire on workflow_human_task (status, expire_time);
