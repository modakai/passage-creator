# 数据库初始化
# @author sakura
# @from sakura

-- 创建库
create database if not exists sakura_boot_init;

-- 切换库
use sakura_boot_init;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    union_id      varchar(256)                           null comment '微信开放平台id',
    mp_open_id     varchar(256)                           null comment '公众号openId',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    status        tinyint      default 1                 not null comment '状态：1启用 0禁用',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除',
    index idx_union_id (union_id)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 字典类型表
create table if not exists sys_dict_type
(
    id          bigint auto_increment comment 'id' primary key,
    dict_code   varchar(128)                           not null comment '字典编码',
    dict_name   varchar(256)                           not null comment '字典名称',
    status      tinyint      default 1                 not null comment '状态：1启用 0禁用',
    remark      varchar(512)                           null comment '备注',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint      default 0                 not null comment '是否删除',
    unique key uk_dict_code (dict_code)
) comment '字典类型' collate = utf8mb4_unicode_ci;

-- 字典明细表
create table if not exists sys_dict_item
(
    id           bigint auto_increment comment 'id' primary key,
    dict_type_id bigint                                not null comment '字典类型id',
    dict_label   varchar(256)                          not null comment '字典标签',
    dict_value   varchar(256)                          not null comment '字典值',
    sort_order   int          default 0                not null comment '排序值',
    status       tinyint      default 1                not null comment '状态：1启用 0禁用',
    tag_type     varchar(64)                           null comment '标签类型',
    remark       varchar(512)                          null comment '备注',
    ext_json     text                                  null comment '扩展JSON',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint      default 0                not null comment '是否删除',
    unique key uk_dict_type_value (dict_type_id, dict_value),
    key idx_dict_type_id (dict_type_id)
) comment '字典明细' collate = utf8mb4_unicode_ci;

-- 协议内容表
create table if not exists sys_agreement
(
    id             bigint auto_increment comment 'id' primary key,
    agreement_type varchar(128)                           not null comment '协议类型编码',
    title          varchar(256)                           not null comment '协议标题',
    content        longtext                               not null comment '协议富文本内容',
    status         tinyint      default 1                 not null comment '状态：1启用 0禁用',
    sort_order     int          default 0                 not null comment '排序值',
    remark         varchar(512)                           null comment '备注',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint      default 0                 not null comment '是否删除',
    unique key uk_agreement_type (agreement_type),
    key idx_status (status),
    key idx_sort_order (sort_order)
) comment '协议内容' collate = utf8mb4_unicode_ci;

-- 文章表
create table if not exists article
(
    id             bigint auto_increment comment 'id' primary key,
    task_id        varchar(64)                           not null comment '任务ID（UUID）',
    user_id        bigint                                not null comment '用户ID',
    topic          varchar(500)                          not null comment '选题',
    main_title     varchar(200)                          null comment '主标题',
    sub_title      varchar(300)                          null comment '副标题',
    outline        json                                  null comment '大纲（JSON格式）',
    content        text                                  null comment '正文（Markdown格式）',
    full_content   text                                  null comment '完整图文（Markdown格式，含配图）',
    cover_image    varchar(512)                          null comment '封面图 URL',
    images         json                                  null comment '配图列表（JSON数组，包含封面图 position=1）',
    status         varchar(20) default 'PENDING'         not null comment '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    error_message  text                                  null comment '错误信息',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    completed_time datetime                              null comment '完成时间',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint     default 0                 not null comment '是否删除',
    unique key uk_task_id (task_id),
    key idx_article_user_id (user_id),
    key idx_article_status (status),
    key idx_article_create_time (create_time),
    key idx_article_user_id_status (user_id, status)
) comment '文章表' collate = utf8mb4_unicode_ci;

-- 系统通知公告表
create table if not exists sys_notification
(
    id             bigint auto_increment comment 'id' primary key,
    type           varchar(32)                            not null comment '类型：message通知/announcement公告',
    title          varchar(256)                           not null comment '标题',
    summary        varchar(512)                           null comment '摘要',
    content        longtext                               not null comment '内容',
    level          varchar(32)  default 'info'            not null comment '级别：info/warning/error',
    status         varchar(32)  default 'draft'           not null comment '状态：draft/published/revoked/archived',
    receiver_type  varchar(32)                            not null comment '接收端：admin/app/all',
    target_type    varchar(32)  default 'all'             not null comment '目标范围：all/role/user',
    pinned         tinyint      default 0                 not null comment '是否置顶',
    popup          tinyint      default 0                 not null comment '是否弹窗',
    link_url       varchar(1024)                          null comment '跳转链接',
    effective_time datetime                               null comment '生效时间',
    expire_time    datetime                               null comment '失效时间',
    publish_time   datetime                               null comment '发布时间',
    publisher_id   bigint                                 null comment '发布人id',
    create_user_id bigint                                 null comment '创建人id',
    update_user_id bigint                                 null comment '更新人id',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint      default 0                 not null comment '是否删除',
    key idx_type_status_receiver (type, status, receiver_type),
    key idx_target_type (target_type),
    key idx_publish_time (publish_time),
    key idx_effective_expire_time (effective_time, expire_time)
) comment '系统通知公告' collate = utf8mb4_unicode_ci;

-- 通知目标表
create table if not exists sys_notification_target
(
    id              bigint auto_increment comment 'id' primary key,
    notification_id bigint                                not null comment '通知id',
    target_type     varchar(32)                           not null comment '目标类型：role/user',
    target_value    varchar(128)                          not null comment '目标值',
    create_time     datetime default CURRENT_TIMESTAMP    not null comment '创建时间',
    is_delete       tinyint  default 0                    not null comment '是否删除',
    key idx_notification_id (notification_id),
    key idx_target (target_type, target_value)
) comment '通知目标' collate = utf8mb4_unicode_ci;

-- 通知阅读状态表
create table if not exists sys_notification_read
(
    id              bigint auto_increment comment 'id' primary key,
    notification_id bigint                                not null comment '通知id',
    receiver_type   varchar(32)                           not null comment '接收端：admin/app',
    user_id         bigint                                not null comment '用户id',
    read_time       datetime                              null comment '已读时间',
    close_time      datetime                              null comment '关闭时间',
    create_time     datetime default CURRENT_TIMESTAMP    not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP    not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint  default 0                    not null comment '是否删除',
    unique key uk_notification_user (notification_id, receiver_type, user_id),
    key idx_user_read (receiver_type, user_id, read_time)
) comment '通知阅读状态' collate = utf8mb4_unicode_ci;

-- 消息通知模板表
create table if not exists sys_notification_template
(
    id              bigint auto_increment comment 'id' primary key,
    template_code   varchar(128)                          not null comment '模板编码',
    event_type      varchar(128)                          not null comment '事件类型',
    title_template  varchar(256)                          not null comment '标题模板',
    content_template longtext                             not null comment '内容模板',
    variable_schema text                                  null comment '变量定义JSON',
    receiver_type   varchar(32)                           not null comment '默认接收端',
    enabled         tinyint  default 0                    not null comment '是否启用',
    remark          varchar(512)                          null comment '备注',
    create_time     datetime default CURRENT_TIMESTAMP    not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP    not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint  default 0                    not null comment '是否删除',
    unique key uk_template_code (template_code),
    key idx_event_enabled (event_type, enabled)
) comment '消息通知模板' collate = utf8mb4_unicode_ci;

-- 审计日志表
create table if not exists sys_audit_log
(
    id                    bigint auto_increment comment 'id' primary key,
    log_type              varchar(32)                            not null comment '日志类型：login/admin_operation',
    user_id               bigint                                 null comment '用户id',
    account_identifier    varchar(256)                           null comment '账号标识',
    ip_address            varchar(64)                            null comment 'IP地址',
    client_info           varchar(512)                           null comment '客户端信息',
    request_path          varchar(512)                           null comment '请求路径',
    http_method           varchar(16)                            null comment 'HTTP方法',
    operation_description varchar(256)                           null comment '操作描述',
    business_module       varchar(128)                           null comment '业务模块',
    operation_type        varchar(64)                            null comment '操作类型',
    cost_millis           bigint                                 null comment '耗时毫秒',
    result                varchar(32)                            not null comment '执行结果：success/failure',
    status_code           int                                    null comment '状态码',
    failure_reason        varchar(512)                           null comment '失败原因',
    exception_summary     varchar(1024)                          null comment '异常摘要',
    request_summary       text                                   null comment '请求摘要',
    response_summary      text                                   null comment '响应摘要',
    trace_id              varchar(128)                           null comment '追踪ID',
    audit_time            datetime     default CURRENT_TIMESTAMP not null comment '审计时间',
    create_time           datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time           datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete             tinyint      default 0                 not null comment '是否删除',
    key idx_log_type_time (log_type, audit_time),
    key idx_user_id (user_id),
    key idx_account_identifier (account_identifier),
    key idx_ip_address (ip_address),
    key idx_request_path (request_path),
    key idx_http_method (http_method),
    key idx_result_time (result, audit_time),
    key idx_operation (business_module, operation_type)
) comment '审计日志' collate = utf8mb4_unicode_ci;

create table if not exists sys_observability_event
(
    id                 bigint                             not null comment 'id' primary key,
    event_type         varchar(64)                        not null comment '事件类型',
    event_level        varchar(32)                        null comment '事件级别',
    title              varchar(128)                       null comment '事件标题',
    subject            varchar(255)                       null comment '事件主体',
    request_path       varchar(512)                       null comment '请求路径',
    http_method        varchar(16)                        null comment 'HTTP方法',
    status_code        int                                null comment '状态码',
    duration_millis    bigint                             null comment '耗时毫秒',
    user_id            bigint                             null comment '用户id',
    account_identifier varchar(128)                       null comment '账号标识',
    ip_address         varchar(64)                        null comment 'IP地址',
    exception_summary  varchar(1024)                      null comment '异常摘要',
    detail             varchar(2000)                      null comment '事件详情',
    audit_log_id       bigint                             null comment '关联审计日志id',
    notification_id    bigint                             null comment '关联通知id',
    event_time         datetime default CURRENT_TIMESTAMP not null comment '事件时间',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete          tinyint  default 0                 not null comment '是否删除',
    key idx_observe_type_time (event_type, event_time),
    key idx_observe_level_time (event_level, event_time),
    key idx_observe_path_time (request_path, event_time),
    key idx_observe_ip_time (ip_address, event_time),
    key idx_observe_account_time (account_identifier, event_time)
) comment '运维观测事件' collate = utf8mb4_unicode_ci;


-- auto-generated definition
create table article
(
    id            bigint auto_increment comment 'id'
        primary key,
    task_id        varchar(64)                           not null comment '任务ID（UUID）',
    user_id        bigint                                not null comment '用户ID',
    topic         varchar(500)                          not null comment '选题',
    main_title     varchar(200)                          null comment '主标题',
    sub_title      varchar(300)                          null comment '副标题',
    outline       json                                  null comment '大纲（JSON格式）',
    content       text                                  null comment '正文（Markdown格式）',
    full_content   text                                  null comment '完整图文（Markdown格式，含配图）',
    cover_image    varchar(512)                          null comment '封面图 URL',
    images        json                                  null comment '配图列表（JSON数组，包含封面图 position=1）',
    status        varchar(20) default 'PENDING'         not null comment '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    error_message  text                                  null comment '错误信息',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    completed_time datetime                              null comment '完成时间',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint     default 0                 not null comment '是否删除',
    constraint uk_taskId
        unique (taskId)
)
    comment '文章表' collate = utf8mb4_unicode_ci;

create index idx_createTime
    on article (create_time);

create index idx_status
    on article (status);

create index idx_userId
    on article (user_id);

create index idx_userId_status
    on article (user_id, status);

alter table article
    add column phase varchar(32) default 'PENDING' not null comment '阶段：PENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING/COMPLETED/FAILED',
    add column title_options json null comment 'AI生成的标题候选',
    add column user_description text null comment '用户选择标题后的补充描述';
