package com.sakura.passage_creator.article.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.article.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 Mapper。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
