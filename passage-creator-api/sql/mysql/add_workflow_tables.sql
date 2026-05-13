-- 通用创作 workflow 表。

create table if not exists workflow_task
(
    id            bigint auto_increment comment 'id' primary key,
    task_id       varchar(64)                            not null comment '任务ID',
    biz_type      varchar(64)                            not null comment '业务类型：article/rednote 等',
    biz_id        bigint                                 null comment '业务主键',
    user_id       bigint                                 not null comment '用户ID',
    status        varchar(32) default 'PENDING'          not null comment '状态',
    current_node  varchar(64)                            null comment '当前节点',
    context_json  json                                   null comment 'Workflow 上下文快照',
    error_message text                                   null comment '错误信息',
    create_time   datetime    default CURRENT_TIMESTAMP  not null comment '创建时间',
    update_time   datetime    default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint     default 0                  not null comment '是否删除',
    unique key uk_workflow_task_id (task_id),
    key idx_workflow_biz (biz_type, biz_id),
    key idx_workflow_user_status (user_id, status),
    key idx_workflow_current_node (current_node)
) comment '通用创作 Workflow 任务表' collate = utf8mb4_unicode_ci;

create table if not exists workflow_human_task
(
    id                  bigint auto_increment comment 'id' primary key,
    task_id             varchar(64)                            not null comment '任务ID',
    biz_type            varchar(64)                            not null comment '业务类型',
    node_type           varchar(64)                            not null comment '人工节点类型',
    status              varchar(32) default 'WAITING'          not null comment '人工任务状态',
    assignee_user_id    bigint                                 not null comment '处理用户ID',
    input_snapshot_json json                                   null comment '展示给用户的输入快照',
    form_schema_json    json                                   null comment '前端表单 schema',
    result_json         json                                   null comment '用户提交结果',
    version             int         default 1                  not null comment '乐观锁版本',
    create_time         datetime    default CURRENT_TIMESTAMP  not null comment '创建时间',
    completed_time      datetime                               null comment '完成时间',
    update_time         datetime    default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete           tinyint     default 0                  not null comment '是否删除',
    key idx_human_task_task_node_status (task_id, node_type, status),
    key idx_human_task_assignee_status (assignee_user_id, status)
) comment '通用创作 Workflow 人工任务表' collate = utf8mb4_unicode_ci;
