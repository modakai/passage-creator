-- Prompt 反馈表，用于按创作环节统计用户对提示词效果的最终评价。
create table if not exists prompt_feedback
(
    id                  bigint                                not null comment '主键 id'
        primary key,
    user_id             bigint                                not null comment '提交反馈的用户 id',
    task_id             varchar(64)                           not null comment '创作任务 id',
    feedback_stage      varchar(32)                           not null comment '反馈环节：文章三环节或 rednote 三个终态 Prompt 环节',
    rating              varchar(16)                           not null comment '评价结果：VERY_SATISFIED/SATISFIED/NEUTRAL/UNSATISFIED',
    remark              varchar(1000)                         null comment '用户可选填写的文字说明',
    prompt_usage_log_id bigint                                null comment '关联的 Prompt 使用日志 id',
    prompt_template_id  bigint                                null comment '使用的 Prompt 模板版本 id',
    template_key        varchar(100)                          null comment 'Prompt 模板标识快照',
    version             varchar(50)                           null comment 'Prompt 模板版本快照',
    environment         varchar(32) default 'default'          null comment 'Prompt 运行环境快照',
    create_time         datetime    default CURRENT_TIMESTAMP  not null comment '创建时间',
    update_time         datetime    default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete           tinyint     default 0                  not null comment '逻辑删除：0-否，1-是',
    unique key uk_prompt_feedback_user_task_stage (user_id, task_id, feedback_stage),
    key idx_prompt_feedback_stage (feedback_stage),
    key idx_prompt_feedback_rating (rating),
    key idx_prompt_feedback_template (template_key, version),
    key idx_prompt_feedback_create_time (create_time),
    key idx_prompt_feedback_usage_log (prompt_usage_log_id)
) comment 'Prompt 反馈表' collate = utf8mb4_unicode_ci;
