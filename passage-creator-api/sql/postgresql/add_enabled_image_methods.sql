-- 为文章任务增加用户选择的配图方式，正文配图阶段会按该列表约束 ImageAgent。
alter table public.article
    add column if not exists enabled_image_methods jsonb;
