-- PostgreSQL 初始化数据脚本。
-- 使用说明：先执行 postgresql/create_table.sql，再执行本脚本。
-- 默认超级管理员账号：sakura，密码：sakura123；生产环境必须修改密码或删除默认账号。

-- 默认超级管理员。密码使用 BCrypt 哈希。
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
    '$2a$10$mm6xH4Opasx37Tm4eKWiVuVZz0LQXN2c4A2L2Nmle6zSP5EYCZWWu',
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

-- 默认 Prompt 模板版本，首次启动后即可在后台查看并发布新版本覆盖。
insert into public.prompt_template (
    template_key,
    version,
    content,
    variables_schema,
    description,
    status,
    environment,
    created_by,
    published_by,
    published_at
) values
(
    'article.title.system',
    '1.0.0',
    $$你是一位爆款文章标题专家,擅长创作吸引人的标题。

能根据用户提供的选题方向,生成 3-5 个爆款文章标题方案:

要求:
1. 每个方案包含主标题和副标题
2. 主标题要包含数字、情绪化词汇,吸引眼球
3. 副标题要补充说明,增强吸引力
4. 标题要简洁有力,不超过30字
5. 不同方案要有不同的切入角度
6. 符合新媒体爆款文章的风格

请直接返回 JSON 格式,不要有其他内容:
[
  {
    "mainTitle": "主标题1",
    "subTitle": "副标题1"
  },
  {
    "mainTitle": "主标题2",
    "subTitle": "副标题2"
  },
  {
    "mainTitle": "主标题3",
    "subTitle": "副标题3"
  }
]$$,
    null,
    '默认标题生成系统 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
),
(
    'article.title.user',
    '1.0.0',
    $$选题：{topic}
$$,
    '[{"name":"topic","label":"选题","required":true}]'::jsonb,
    '默认标题生成用户 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
),
(
    'article.outline.system',
    '1.0.0',
    $$你是一位专业的文章策划师,擅长设计文章结构。

根据提供的主标题、副标题和补充描述[可选，用户提供就用，没提供就不管]，生成文章的大纲

要求:
1. 大纲要有清晰的逻辑结构
2. 包含开头引入、核心观点(3-5个)、结尾升华
3. 每个章节要有明确的标题和核心要点(2-3个)
4. 适合2000字左右，但不要超过3000字的文章
5. 所有 JSON 字符串值必须使用英文双引号包裹，不能省略引号

请直接返回 JSON 格式,不要有其他内容:
{
  "sections": [
    {
      "section": 1,
      "title": "章节标题",
      "points": ["要点1", "要点2"]
    }
  ]
}$$,
    null,
    '默认大纲生成系统 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
),
(
    'article.outline.user',
    '1.0.0',
    $$根据以下标题,生成文章大纲:
主标题：{mainTitle}
副标题：{subTitle}
{descriptionSection}
{format}$$,
    '[
      {"name":"mainTitle","label":"主标题","required":true},
      {"name":"subTitle","label":"副标题","required":true},
      {"name":"descriptionSection","label":"补充描述","required":true},
      {"name":"format","label":"结构化输出格式","required":true}
    ]'::jsonb,
    '默认大纲生成用户 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
),
(
    'article.content.system',
    '1.0.0',
    $$你是一位资深的内容创作者,擅长撰写优质文章。

根据用户提供的大纲、标题,创作文章正文，具体有：
主标题、副标题、大纲

要求:
1. 内容要充实,每个章节300-400字
2. 语言流畅,富有感染力
3. 适当使用金句,增强可读性
4. 添加过渡句,确保逻辑连贯
5. 使用 Markdown 格式,章节使用 ## 标题

请直接返回 Markdown 格式的正文内容,不要有其他内容。$$,
    null,
    '默认正文生成系统 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
),
(
    'article.content.user',
    '1.0.0',
    $$根据以下大纲,创作文章正文:
主标题：{mainTitle}
副标题：{subTitle}
大纲：
{outline}
$$,
    '[
      {"name":"mainTitle","label":"主标题","required":true},
      {"name":"subTitle","label":"副标题","required":true},
      {"name":"outline","label":"大纲","required":true}
    ]'::jsonb,
    '默认正文生成用户 Prompt',
    'ACTIVE',
    'production',
    'system',
    'system',
    current_timestamp
)
on conflict (template_key, version, environment) do update set
    content = excluded.content,
    variables_schema = excluded.variables_schema,
    description = excluded.description;

-- AI 模型默认费率，积分单价可在后台后续扩展为配置页面。
insert into public.ai_model_pricing (
    provider, model, request_type, prompt_token_price_per1k, completion_token_price_per1k,
    fixed_credits, reserve_credits, enabled
)
values
('DASHSCOPE', 'qwen3-max', 'TEXT', 0.002000, 0.006000, 0.0000, 1.0000, 1),
('OPENAI', 'gpt-image-2', 'IMAGE', 0.000000, 0.000000, 5.0000, 5.0000, 1)
on conflict (provider, model, request_type) do update set
    prompt_token_price_per1k = excluded.prompt_token_price_per1k,
    completion_token_price_per1k = excluded.completion_token_price_per1k,
    fixed_credits = excluded.fixed_credits,
    reserve_credits = excluded.reserve_credits,
    enabled = excluded.enabled;
