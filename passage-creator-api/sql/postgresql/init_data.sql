-- PostgreSQL 初始化数据脚本。
-- 使用说明：先执行 postgresql/create_table.sql，再执行本脚本。
-- 默认超级管理员账号：sakura，密码：12345678；生产环境必须修改密码或删除默认账号。

-- 默认超级管理员。密码算法与 UserConstant.PASSWORD_SALT 保持一致。
insert into public."user" (
    id,
    user_account,
    user_password,
    user_name,
    user_profile,
    user_role,
    status,
    is_delete
) values (
    1,
    'sakura',
    '2a6dd3323691b39e8e9b1132b035ede5',
    'Sakura Admin',
    'PROTECTED_SUPER_ADMIN_ACCOUNT：模板内置超级管理员，仅用于本地初始化。',
    'admin',
    1,
    0
) on conflict (id) do update set
    user_account = excluded.user_account,
    user_password = excluded.user_password,
    user_name = excluded.user_name,
    user_profile = excluded.user_profile,
    user_role = excluded.user_role,
    status = excluded.status,
    is_delete = excluded.is_delete;

-- 保证后续自增 ID 不会和默认管理员冲突。
select setval(pg_get_serial_sequence('public."user"', 'id'), greatest((select max(id) from public."user"), 1), true);

-- 常用字典类型。
insert into public.sys_dict_type (dict_code, dict_name, status, remark, is_delete)
values
    ('user_role', '用户角色', 1, '系统内置用户角色字典', 0),
    ('common_status', '通用状态', 1, '启用、禁用等通用状态', 0),
    ('notice_level', '通知级别', 1, '通知公告展示级别', 0),
    ('receiver_type', '接收端类型', 1, '通知公告接收端范围', 0)
on conflict (dict_code) do update set
    dict_name = excluded.dict_name,
    status = excluded.status,
    remark = excluded.remark,
    is_delete = excluded.is_delete;

-- 用户角色字典项。
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '管理员', 'admin', 10, 1, 'destructive', '后台管理员角色', 0 from public.sys_dict_type where dict_code = 'user_role'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '普通用户', 'user', 20, 1, 'default', '默认用户角色', 0 from public.sys_dict_type where dict_code = 'user_role'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '封禁用户', 'ban', 30, 1, 'secondary', '禁止登录角色', 0 from public.sys_dict_type where dict_code = 'user_role'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;

-- 通用状态字典项。
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '启用', '1', 10, 1, 'default', '可用状态', 0 from public.sys_dict_type where dict_code = 'common_status'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '禁用', '0', 20, 1, 'secondary', '不可用状态', 0 from public.sys_dict_type where dict_code = 'common_status'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;

-- 通知级别字典项。
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '普通', 'info', 10, 1, 'default', '普通通知', 0 from public.sys_dict_type where dict_code = 'notice_level'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '警告', 'warning', 20, 1, 'secondary', '需要关注的通知', 0 from public.sys_dict_type where dict_code = 'notice_level'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '错误', 'error', 30, 1, 'destructive', '异常或失败通知', 0 from public.sys_dict_type where dict_code = 'notice_level'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;

-- 接收端字典项。
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '后台用户', 'admin', 10, 1, 'default', '后台管理端用户', 0 from public.sys_dict_type where dict_code = 'receiver_type'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '用户端用户', 'app', 20, 1, 'secondary', '用户端用户', 0 from public.sys_dict_type where dict_code = 'receiver_type'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;
insert into public.sys_dict_item (dict_type_id, dict_label, dict_value, sort_order, status, tag_type, remark, is_delete)
select id, '全部用户', 'all', 30, 1, 'outline', '后台和用户端全部用户', 0 from public.sys_dict_type where dict_code = 'receiver_type'
on conflict (dict_type_id, dict_value) do update set dict_label = excluded.dict_label, sort_order = excluded.sort_order, status = excluded.status, tag_type = excluded.tag_type, remark = excluded.remark, is_delete = excluded.is_delete;

-- 默认协议内容。
insert into public.sys_agreement (agreement_type, title, content, status, sort_order, remark, is_delete)
values
    ('user_terms', '用户协议', '<h1>用户协议</h1><p>这里是模板默认用户协议，请在正式上线前替换为真实内容。</p>', 1, 10, '模板默认协议', 0),
    ('privacy_policy', '隐私政策', '<h1>隐私政策</h1><p>这里是模板默认隐私政策，请在正式上线前替换为真实内容。</p>', 1, 20, '模板默认协议', 0)
on conflict (agreement_type) do update set
    title = excluded.title,
    content = excluded.content,
    status = excluded.status,
    sort_order = excluded.sort_order,
    remark = excluded.remark,
    is_delete = excluded.is_delete;

-- 默认公告，便于首次进入后台时验证通知公告能力。
insert into public.sys_notification (
    type,
    title,
    summary,
    content,
    level,
    status,
    receiver_type,
    target_type,
    pinned,
    popup,
    effective_time,
    publish_time,
    publisher_id,
    create_user_id,
    update_user_id,
    is_delete
)
select
    'announcement',
    '欢迎使用 Sakura Admin',
    '模板初始化公告',
    '<p>项目已完成初始化，请及时修改默认管理员密码并按需调整基础配置。</p>',
    'info',
    'published',
    'admin',
    'all',
    1,
    0,
    current_timestamp,
    current_timestamp,
    1,
    1,
    1,
    0
where not exists (
    select 1 from public.sys_notification where type = 'announcement' and title = '欢迎使用 Sakura Admin' and is_delete = 0
);

-- 默认通知模板，用于禁用用户等系统事件。
insert into public.sys_notification_template (
    template_code,
    event_type,
    title_template,
    content_template,
    variable_schema,
    receiver_type,
    enabled,
    remark,
    is_delete
) values (
    'USER_DISABLED',
    'user.disabled',
    '账号已被禁用',
    '你的账号已被管理员禁用，原因：{reason}',
    '[{"name":"reason","label":"禁用原因","required":true}]',
    'app',
    1,
    '模板初始化数据',
    0
) on conflict (template_code) do update set
    event_type = excluded.event_type,
    title_template = excluded.title_template,
    content_template = excluded.content_template,
    variable_schema = excluded.variable_schema,
    receiver_type = excluded.receiver_type,
    enabled = excluded.enabled,
    remark = excluded.remark,
    is_delete = excluded.is_delete;
