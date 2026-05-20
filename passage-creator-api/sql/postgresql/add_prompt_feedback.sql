-- Prompt 反馈表，用于按创作环节统计用户对提示词效果的最终评价。
create table if not exists public.prompt_feedback
(
    id                  bigint       not null primary key,
    user_id             bigint       not null,
    task_id             varchar(64)  not null,
    feedback_stage      varchar(32)  not null,
    rating              varchar(16)  not null,
    remark              varchar(1000),
    prompt_usage_log_id bigint,
    prompt_template_id  bigint,
    template_key        varchar(100),
    version             varchar(50),
    environment         varchar(32) default 'default',
    create_time         timestamp   default CURRENT_TIMESTAMP not null,
    update_time         timestamp   default CURRENT_TIMESTAMP not null,
    is_delete           smallint    default 0 not null,
    constraint uk_prompt_feedback_user_task_stage unique (user_id, task_id, feedback_stage)
);

comment on table public.prompt_feedback is 'Prompt 反馈表';
comment on column public.prompt_feedback.user_id is '提交反馈的用户 id';
comment on column public.prompt_feedback.task_id is '创作任务 id';
comment on column public.prompt_feedback.feedback_stage is '反馈环节：文章三环节或 rednote 三个终态 Prompt 环节';
comment on column public.prompt_feedback.rating is '评价结果：VERY_SATISFIED/SATISFIED/NEUTRAL/UNSATISFIED';
comment on column public.prompt_feedback.remark is '用户可选填写的文字说明';
comment on column public.prompt_feedback.prompt_usage_log_id is '关联的 Prompt 使用日志 id';
comment on column public.prompt_feedback.prompt_template_id is '使用的 Prompt 模板版本 id';
comment on column public.prompt_feedback.template_key is 'Prompt 模板标识快照';
comment on column public.prompt_feedback.version is 'Prompt 模板版本快照';
comment on column public.prompt_feedback.environment is 'Prompt 运行环境快照';

create index if not exists idx_prompt_feedback_stage on public.prompt_feedback (feedback_stage);
create index if not exists idx_prompt_feedback_rating on public.prompt_feedback (rating);
create index if not exists idx_prompt_feedback_template on public.prompt_feedback (template_key, version);
create index if not exists idx_prompt_feedback_create_time on public.prompt_feedback (create_time);
create index if not exists idx_prompt_feedback_usage_log on public.prompt_feedback (prompt_usage_log_id);
