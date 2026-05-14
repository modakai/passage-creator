-- 通用创作 workflow 表。

create table if not exists public.workflow_task
(
    id            bigserial primary key,
    task_id       varchar(64)                           not null,
    biz_type      varchar(64)                           not null,
    biz_id        bigint,
    user_id       bigint                                not null,
    status        varchar(32) default 'PENDING'         not null,
    current_node  varchar(64),
    context_json  jsonb,
    error_message text,
    create_time   timestamp   default current_timestamp not null,
    update_time   timestamp   default current_timestamp not null,
    is_delete     smallint    default 0                 not null,
    constraint uk_workflow_task_id unique (task_id)
);
create index if not exists idx_workflow_biz on public.workflow_task (biz_type, biz_id);
create index if not exists idx_workflow_user_status on public.workflow_task (user_id, status);
create index if not exists idx_workflow_current_node on public.workflow_task (current_node);

create table if not exists public.workflow_human_task
(
    id                  bigserial primary key,
    task_id             varchar(64)                           not null,
    biz_type            varchar(64)                           not null,
    node_type           varchar(64)                           not null,
    status              varchar(32) default 'WAITING'         not null,
    assignee_user_id    bigint                                not null,
    input_snapshot_json jsonb,
    form_schema_json    jsonb,
    result_json         jsonb,
    version             integer     default 1                 not null,
    create_time         timestamp   default current_timestamp not null,
    expire_time         timestamp,
    completed_time      timestamp,
    update_time         timestamp   default current_timestamp not null,
    is_delete           smallint    default 0                 not null
);
create index if not exists idx_human_task_task_node_status on public.workflow_human_task (task_id, node_type, status);
create index if not exists idx_human_task_assignee_status on public.workflow_human_task (assignee_user_id, status);
create index if not exists idx_human_task_status_expire on public.workflow_human_task (status, expire_time);
