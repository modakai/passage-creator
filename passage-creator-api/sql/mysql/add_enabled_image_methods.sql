-- 为文章任务增加用户选择的配图方式，正文配图阶段会按该列表约束 ImageAgent。
alter table article
    add column enabled_image_methods json null comment '用户允许使用的配图方式' after images;
