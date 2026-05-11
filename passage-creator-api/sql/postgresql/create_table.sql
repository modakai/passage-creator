-- PostgreSQL 数据库初始化脚本。
-- 使用说明：先创建并连接 sakura_boot_init 数据库，再执行本脚本。
-- 示例：createdb sakura_boot_init && psql -d sakura_boot_init -f springboot3_init/sql/postgresql/create_table.sql

create schema if not exists public;

-- 用户表。user 是 PostgreSQL 关键字相关名称，因此这里使用双引号固定表名。
create table if not exists public."user"
(
    id            bigserial primary key,
    user_account  varchar(256)                           not null,
    user_password varchar(512)                           not null,
    union_id      varchar(256),
    mp_open_id    varchar(256),
    user_name     varchar(256),
    user_avatar   varchar(1024),
    user_profile  varchar(512),
    user_role     varchar(256) default 'user'            not null,
    status        smallint     default 1                 not null,
    create_time   timestamp    default current_timestamp not null,
    update_time   timestamp    default current_timestamp not null,
    is_delete     smallint     default 0                 not null
);
create index if not exists idx_user_union_id on public."user" (union_id);

-- 字典类型表。
create table if not exists public.sys_dict_type
(
    id          bigserial primary key,
    dict_code   varchar(128)                           not null,
    dict_name   varchar(256)                           not null,
    status      smallint     default 1                 not null,
    remark      varchar(512),
    create_time timestamp    default current_timestamp not null,
    update_time timestamp    default current_timestamp not null,
    is_delete   smallint     default 0                 not null,
    constraint uk_dict_code unique (dict_code)
);

-- 字典明细表。
create table if not exists public.sys_dict_item
(
    id           bigserial primary key,
    dict_type_id bigint                                not null,
    dict_label   varchar(256)                          not null,
    dict_value   varchar(256)                          not null,
    sort_order   integer    default 0                  not null,
    status       smallint   default 1                  not null,
    tag_type     varchar(64),
    remark       varchar(512),
    ext_json     text,
    create_time  timestamp  default current_timestamp  not null,
    update_time  timestamp  default current_timestamp  not null,
    is_delete    smallint   default 0                  not null,
    constraint uk_dict_type_value unique (dict_type_id, dict_value)
);
create index if not exists idx_dict_type_id on public.sys_dict_item (dict_type_id);

-- 协议内容表。
create table if not exists public.sys_agreement
(
    id             bigserial primary key,
    agreement_type varchar(128)                           not null,
    title          varchar(256)                           not null,
    content        text                                   not null,
    status         smallint     default 1                 not null,
    sort_order     integer      default 0                 not null,
    remark         varchar(512),
    create_time    timestamp    default current_timestamp not null,
    update_time    timestamp    default current_timestamp not null,
    is_delete      smallint     default 0                 not null,
    constraint uk_agreement_type unique (agreement_type)
);
create index if not exists idx_sys_agreement_status on public.sys_agreement (status);
create index if not exists idx_sys_agreement_sort_order on public.sys_agreement (sort_order);

-- 文章表。
create table if not exists public.article
(
    id             bigserial primary key,
    task_id        varchar(64)                           not null,
    user_id        bigint                                not null,
    topic          varchar(500)                          not null,
    main_title     varchar(200),
    sub_title      varchar(300),
    outline        jsonb,
    content        text,
    full_content   text,
    cover_image    varchar(512),
    images         jsonb,
    enabled_image_methods jsonb,
    status         varchar(20) default 'PENDING'         not null,
    error_message  text,
    create_time    timestamp   default current_timestamp not null,
    completed_time timestamp,
    update_time    timestamp   default current_timestamp not null,
    is_delete      smallint    default 0                 not null,
    constraint uk_task_id unique (task_id)
);
create index if not exists idx_article_user_id on public.article (user_id);
create index if not exists idx_article_status on public.article (status);
create index if not exists idx_article_create_time on public.article (create_time);
create index if not exists idx_article_user_id_status on public.article (user_id, status);

-- 系统通知公告表。
create table if not exists public.sys_notification
(
    id             bigserial primary key,
    type           varchar(32)                            not null,
    title          varchar(256)                           not null,
    summary        varchar(512),
    content        text                                   not null,
    level          varchar(32)  default 'info'            not null,
    status         varchar(32)  default 'draft'           not null,
    receiver_type  varchar(32)                            not null,
    target_type    varchar(32)  default 'all'             not null,
    pinned         smallint     default 0                 not null,
    popup          smallint     default 0                 not null,
    link_url       varchar(1024),
    effective_time timestamp,
    expire_time    timestamp,
    publish_time   timestamp,
    publisher_id   bigint,
    create_user_id bigint,
    update_user_id bigint,
    create_time    timestamp    default current_timestamp not null,
    update_time    timestamp    default current_timestamp not null,
    is_delete      smallint     default 0                 not null
);
create index if not exists idx_notification_type_status_receiver on public.sys_notification (type, status, receiver_type);
create index if not exists idx_notification_target_type on public.sys_notification (target_type);
create index if not exists idx_notification_publish_time on public.sys_notification (publish_time);
create index if not exists idx_notification_effective_expire_time on public.sys_notification (effective_time, expire_time);

-- 通知目标表。
create table if not exists public.sys_notification_target
(
    id              bigserial primary key,
    notification_id bigint                               not null,
    target_type     varchar(32)                          not null,
    target_value    varchar(128)                         not null,
    create_time     timestamp default current_timestamp  not null,
    is_delete       smallint  default 0                  not null
);
create index if not exists idx_notification_target_notification_id on public.sys_notification_target (notification_id);
create index if not exists idx_notification_target on public.sys_notification_target (target_type, target_value);

-- 通知阅读状态表。
create table if not exists public.sys_notification_read
(
    id              bigserial primary key,
    notification_id bigint                               not null,
    receiver_type   varchar(32)                          not null,
    user_id         bigint                               not null,
    read_time       timestamp,
    close_time      timestamp,
    create_time     timestamp default current_timestamp  not null,
    update_time     timestamp default current_timestamp  not null,
    is_delete       smallint  default 0                  not null,
    constraint uk_notification_user unique (notification_id, receiver_type, user_id)
);
create index if not exists idx_notification_user_read on public.sys_notification_read (receiver_type, user_id, read_time);

-- 消息通知模板表。
create table if not exists public.sys_notification_template
(
    id               bigserial primary key,
    template_code    varchar(128)                         not null,
    event_type       varchar(128)                         not null,
    title_template   varchar(256)                         not null,
    content_template text                                 not null,
    variable_schema  text,
    receiver_type    varchar(32)                          not null,
    enabled          smallint  default 0                  not null,
    remark           varchar(512),
    create_time      timestamp default current_timestamp  not null,
    update_time      timestamp default current_timestamp  not null,
    is_delete        smallint  default 0                  not null,
    constraint uk_template_code unique (template_code)
);
create index if not exists idx_template_event_enabled on public.sys_notification_template (event_type, enabled);

-- Prompt 模板版本表。
create table if not exists public.prompt_template
(
    id               bigserial primary key,
    template_key     varchar(100)                         not null,
    version          varchar(20)                          not null,
    content          text                                 not null,
    variables_schema jsonb,
    description      text,
    status           varchar(20) default 'DRAFT'          not null,
    environment      varchar(20) default 'production'     not null,
    created_by       varchar(100),
    published_by     varchar(100),
    published_at     timestamp,
    created_at       timestamp   default current_timestamp not null,
    updated_at       timestamp   default current_timestamp not null,
    constraint uk_prompt_key_version_env unique (template_key, version, environment)
);
create unique index if not exists uk_prompt_key_env_active
    on public.prompt_template (template_key, environment)
    where status = 'ACTIVE';
create index if not exists idx_prompt_key_env_status on public.prompt_template (template_key, environment, status);
create index if not exists idx_prompt_updated_at on public.prompt_template (updated_at);

-- Prompt 使用日志表。
create table if not exists public.prompt_usage_log
(
    id                 bigserial primary key,
    prompt_template_id bigint,
    template_key       varchar(100)                         not null,
    version            varchar(20)                          not null,
    environment        varchar(20)                          not null,
    agent_name         varchar(100)                         not null,
    task_id            varchar(100),
    session_id         varchar(100),
    user_id            bigint,
    used_at            timestamp default current_timestamp  not null,
    response_ok        boolean,
    error_message      text,
    latency_ms         integer,
    feedback           integer
);
create index if not exists idx_prompt_usage_template_time on public.prompt_usage_log (template_key, used_at);
create index if not exists idx_prompt_usage_agent_time on public.prompt_usage_log (agent_name, used_at);
create index if not exists idx_prompt_usage_task_id on public.prompt_usage_log (task_id);
create index if not exists idx_prompt_usage_template_id on public.prompt_usage_log (prompt_template_id);

-- AI 模型费率配置表。
create table if not exists public.ai_model_pricing
(
    id                           bigserial primary key,
    provider                     varchar(64)                            not null,
    model                        varchar(128)                           not null,
    request_type                 varchar(32)                            not null,
    prompt_token_price_per1k     numeric(18, 6) default 0.000000        not null,
    completion_token_price_per1k numeric(18, 6) default 0.000000        not null,
    fixed_credits                numeric(18, 4) default 0.0000          not null,
    reserve_credits              numeric(18, 4) default 1.0000          not null,
    enabled                      smallint       default 1               not null,
    create_time                  timestamp      default current_timestamp not null,
    update_time                  timestamp      default current_timestamp not null,
    is_delete                    smallint       default 0               not null,
    constraint uk_ai_pricing_provider_model_type unique (provider, model, request_type)
);
create index if not exists idx_ai_pricing_enabled on public.ai_model_pricing (enabled);

-- AI 用量记录表。
create table if not exists public.ai_usage_record
(
    id                bigserial primary key,
    user_id           bigint                                not null,
    task_id           varchar(100),
    agent_name        varchar(100)                          not null,
    phase             varchar(64),
    provider          varchar(64)                           not null,
    model             varchar(128)                          not null,
    request_type      varchar(32)                           not null,
    prompt_tokens     bigint      default 0                 not null,
    completion_tokens bigint      default 0                 not null,
    total_tokens      bigint      default 0                 not null,
    credit_cost       numeric(18, 4) default 0.0000         not null,
    latency_ms        integer,
    response_ok       boolean,
    error_message     text,
    used_at           timestamp   default current_timestamp not null,
    create_time       timestamp   default current_timestamp not null,
    update_time       timestamp   default current_timestamp not null,
    is_delete         smallint    default 0                 not null
);
create index if not exists idx_ai_usage_user_time on public.ai_usage_record (user_id, used_at);
create index if not exists idx_ai_usage_task_id on public.ai_usage_record (task_id);
create index if not exists idx_ai_usage_model_time on public.ai_usage_record (provider, model, used_at);
create index if not exists idx_ai_usage_phase_time on public.ai_usage_record (phase, used_at);

-- 用户积分账户表。
create table if not exists public.credit_account
(
    id             bigserial primary key,
    user_id        bigint                                not null,
    balance        numeric(18, 4) default 0.0000         not null,
    total_recharge numeric(18, 4) default 0.0000         not null,
    total_consume  numeric(18, 4) default 0.0000         not null,
    create_time    timestamp   default current_timestamp not null,
    update_time    timestamp   default current_timestamp not null,
    is_delete      smallint    default 0                 not null,
    constraint uk_credit_account_user unique (user_id)
);

-- 用户积分流水表。
create table if not exists public.credit_transaction
(
    id               bigserial primary key,
    user_id          bigint                                not null,
    account_id       bigint                                not null,
    transaction_type varchar(32)                           not null,
    status           varchar(32)                           not null,
    amount           numeric(18, 4) default 0.0000         not null,
    balance_after    numeric(18, 4) default 0.0000         not null,
    biz_type         varchar(64),
    biz_id           varchar(128),
    description      varchar(512),
    operator         varchar(100),
    create_time      timestamp    default current_timestamp not null,
    update_time      timestamp    default current_timestamp not null,
    is_delete        smallint     default 0                 not null
);
create index if not exists idx_credit_tx_user_time on public.credit_transaction (user_id, create_time);
create index if not exists idx_credit_tx_biz on public.credit_transaction (biz_type, biz_id);
create index if not exists idx_credit_tx_status on public.credit_transaction (status);

-- 审计日志表。
create table if not exists public.sys_audit_log
(
    id                    bigserial primary key,
    log_type              varchar(32)                            not null,
    user_id               bigint,
    account_identifier    varchar(256),
    ip_address            varchar(64),
    client_info           varchar(512),
    request_path          varchar(512),
    http_method           varchar(16),
    operation_description varchar(256),
    business_module       varchar(128),
    operation_type        varchar(64),
    cost_millis           bigint,
    result                varchar(32)                            not null,
    status_code           integer,
    failure_reason        varchar(512),
    exception_summary     varchar(1024),
    request_summary       text,
    response_summary      text,
    trace_id              varchar(128),
    audit_time            timestamp    default current_timestamp not null,
    create_time           timestamp    default current_timestamp not null,
    update_time           timestamp    default current_timestamp not null,
    is_delete             smallint     default 0                 not null
);
create index if not exists idx_audit_log_type_time on public.sys_audit_log (log_type, audit_time);
create index if not exists idx_audit_user_id on public.sys_audit_log (user_id);
create index if not exists idx_audit_account_identifier on public.sys_audit_log (account_identifier);
create index if not exists idx_audit_ip_address on public.sys_audit_log (ip_address);
create index if not exists idx_audit_request_path on public.sys_audit_log (request_path);
create index if not exists idx_audit_http_method on public.sys_audit_log (http_method);
create index if not exists idx_audit_result_time on public.sys_audit_log (result, audit_time);
create index if not exists idx_audit_operation on public.sys_audit_log (business_module, operation_type);

create table if not exists public.sys_observability_event
(
    id                 bigint primary key,
    event_type         varchar(64) not null,
    event_level        varchar(32),
    title              varchar(128),
    subject            varchar(255),
    request_path       varchar(512),
    http_method        varchar(16),
    status_code        integer,
    duration_millis    bigint,
    user_id            bigint,
    account_identifier varchar(128),
    ip_address         varchar(64),
    exception_summary  varchar(1024),
    detail             varchar(2000),
    audit_log_id       bigint,
    notification_id    bigint,
    event_time         timestamp default CURRENT_TIMESTAMP not null,
    create_time        timestamp default CURRENT_TIMESTAMP not null,
    update_time        timestamp default CURRENT_TIMESTAMP not null,
    is_delete          smallint  default 0 not null
);
comment on table public.sys_observability_event is '运维观测事件';
comment on column public.sys_observability_event.event_type is '事件类型';
comment on column public.sys_observability_event.event_level is '事件级别';
comment on column public.sys_observability_event.title is '事件标题';
comment on column public.sys_observability_event.subject is '事件主体';
comment on column public.sys_observability_event.request_path is '请求路径';
comment on column public.sys_observability_event.http_method is 'HTTP方法';
comment on column public.sys_observability_event.status_code is '状态码';
comment on column public.sys_observability_event.duration_millis is '耗时毫秒';
comment on column public.sys_observability_event.user_id is '用户id';
comment on column public.sys_observability_event.account_identifier is '账号标识';
comment on column public.sys_observability_event.ip_address is 'IP地址';
comment on column public.sys_observability_event.exception_summary is '异常摘要';
comment on column public.sys_observability_event.detail is '事件详情';
comment on column public.sys_observability_event.audit_log_id is '关联审计日志id';
comment on column public.sys_observability_event.notification_id is '关联通知id';
comment on column public.sys_observability_event.event_time is '事件时间';
create index if not exists idx_observe_type_time on public.sys_observability_event (event_type, event_time);
create index if not exists idx_observe_level_time on public.sys_observability_event (event_level, event_time);
create index if not exists idx_observe_path_time on public.sys_observability_event (request_path, event_time);
create index if not exists idx_observe_ip_time on public.sys_observability_event (ip_address, event_time);
create index if not exists idx_observe_account_time on public.sys_observability_event (account_identifier, event_time);
