-- 为已存在的 workflow_human_task 表补充人工任务过期时间。
-- 如果是全新库，add_workflow_tables.sql 已包含该字段；老库需要单独执行本脚本。

alter table public.workflow_human_task
    add column if not exists expire_time timestamp;

create index if not exists idx_human_task_status_expire on public.workflow_human_task (status, expire_time);
