-- 小红书爆款笔记表，用于保存 rednote 全自动 workflow 的输入、阶段结果和图片产物。
create table if not exists public.rednote_note
(
    id                 bigint       not null primary key,
    task_id            varchar(64)  not null,
    user_id            bigint       not null,
    content            text         not null,
    subject            varchar(300),
    context            text,
    content_length     varchar(32),
    target_word_count  integer,
    keywords           jsonb,
    tag_count          integer      default 5 not null,
    image_count        integer      default 3 not null,
    search_results     jsonb,
    body_content       text,
    tags               jsonb,
    cover_title        varchar(200),
    cover_prompt       text,
    image_prompts      jsonb,
    images             jsonb,
    cover_image        varchar(512),
    status             varchar(32) default 'PENDING' not null,
    phase              varchar(64) default 'PENDING' not null,
    error_message      text,
    create_time        timestamp   default CURRENT_TIMESTAMP not null,
    completed_time     timestamp,
    update_time        timestamp   default CURRENT_TIMESTAMP not null,
    is_delete          smallint    default 0 not null,
    constraint uk_rednote_task_id unique (task_id)
);

comment on table public.rednote_note is '小红书爆款笔记表';
comment on column public.rednote_note.id is '主键 id';
comment on column public.rednote_note.task_id is 'Workflow 任务 ID（UUID）';
comment on column public.rednote_note.user_id is '创建用户 id';
comment on column public.rednote_note.content is '用户原始自然语言创作需求';
comment on column public.rednote_note.subject is 'SearchAgent 解析出的核心主体/产品/场景';
comment on column public.rednote_note.context is 'SearchAgent 整理后的创作上下文';
comment on column public.rednote_note.content_length is '篇幅档位：SHORT/MEDIUM/LONG';
comment on column public.rednote_note.target_word_count is '目标字数';
comment on column public.rednote_note.keywords is '关键词列表 JSON';
comment on column public.rednote_note.tag_count is '标签数量，默认 5';
comment on column public.rednote_note.image_count is '普通配图数量，最多 5，不含封面';
comment on column public.rednote_note.search_results is '搜索结果摘要 JSON';
comment on column public.rednote_note.body_content is '小红书正文主体';
comment on column public.rednote_note.tags is '标签列表 JSON';
comment on column public.rednote_note.cover_title is '封面标题';
comment on column public.rednote_note.cover_prompt is '封面图片提示词';
comment on column public.rednote_note.image_prompts is '普通配图提示词计划 JSON';
comment on column public.rednote_note.images is '配图结果列表 JSON，包含 URL、位置、状态和失败原因';
comment on column public.rednote_note.cover_image is '封面图 URL';
comment on column public.rednote_note.status is '状态：PENDING/PROCESSING/COMPLETED/FAILED';
comment on column public.rednote_note.phase is '阶段：PENDING/SEARCH_AGENT/COPY_GENERATING/IMAGE_PROMPT_GENERATING/IMAGE_GENERATING/COMPLETED/FAILED';
comment on column public.rednote_note.error_message is '失败错误信息';
comment on column public.rednote_note.create_time is '创建时间';
comment on column public.rednote_note.completed_time is '完成时间';
comment on column public.rednote_note.update_time is '更新时间';
comment on column public.rednote_note.is_delete is '逻辑删除：0-否，1-是';

create index if not exists idx_rednote_user_time on public.rednote_note (user_id, create_time);
create index if not exists idx_rednote_status on public.rednote_note (status);
create index if not exists idx_rednote_phase on public.rednote_note (phase);
create index if not exists idx_rednote_subject on public.rednote_note (subject);
create index if not exists idx_rednote_user_status on public.rednote_note (user_id, status);
