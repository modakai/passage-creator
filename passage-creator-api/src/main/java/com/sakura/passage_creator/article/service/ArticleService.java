package com.sakura.passage_creator.article.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.dto.ArticleAddRequest;
import com.sakura.passage_creator.article.model.dto.ArticleQueryRequest;
import com.sakura.passage_creator.article.model.dto.ArticleUpdateRequest;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.model.vo.ArticleVO;
import com.sakura.passage_creator.shared.context.LoginUserInfo;

import java.util.List;

/**
 * 文章服务。
 */
public interface ArticleService extends IService<Article> {

    /**
     * 新增文章。
     *
     * @param request   新增请求
     * @param loginUser 当前登录用户
     * @return 新增文章 id
     */
    Long addArticle(ArticleAddRequest request, LoginUserInfo loginUser);

    /**
     * 更新文章。
     *
     * @param request   更新请求
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean updateArticle(ArticleUpdateRequest request, LoginUserInfo loginUser);

    /**
     * 删除文章。
     *
     * @param id        文章 id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean removeArticle(Long id, LoginUserInfo loginUser);

    /**
     * 获取文章详情。
     *
     * @param id        文章 id
     * @param loginUser 当前登录用户
     * @return 文章详情
     */
    Article getAccessibleArticle(Long id, LoginUserInfo loginUser);

    /**
     * 构造查询条件。
     *
     * @param queryRequest 查询请求
     * @param loginUser    当前登录用户
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ArticleQueryRequest queryRequest, LoginUserInfo loginUser);

    /**
     * 校验当前用户是否可访问文章。
     *
     * @param article   文章实体
     * @param loginUser 当前登录用户
     */
    void assertArticleAccessible(Article article, LoginUserInfo loginUser);

    /**
     * 转换为文章展示对象。
     *
     * @param article 文章实体
     * @return 展示对象
     */
    ArticleVO getArticleVO(Article article);

    /**
     * 批量转换为文章展示对象。
     *
     * @param articleList 文章实体列表
     * @return 展示对象列表
     */
    List<ArticleVO> getArticleVO(List<Article> articleList);

    /**
     * 创建文章 （todo 后续扩展 配置检测）
     *
     * @param topic     选提
     * @param loginUser 登录用户
     * @return 任务id
     */
    String createArticle(String topic, LoginUserInfo loginUser);

    /**
     * 更新文章状态
     *
     * @param articleStatusEnum 枚举值
     * @param taskId            任务id
     * @return 是否成功
     */
    Boolean updateStatus(ArticleStatusEnum articleStatusEnum, String taskId);

    /**
     * 更新文章的阶段状态
     *
     * @param articlePhaseEnum 阶段枚举
     * @param taskId           任务id
     * @return 是否成功
     */
    boolean updatePhase(ArticlePhaseEnum articlePhaseEnum, String taskId);

    /**
     * 保存标题方案
     *
     * @param titleOptions 标题方案
     * @param taskId       任务id
     * @return 是否成功
     */
    Boolean saveTitleOptions(List<ArticleState.TitleOption> titleOptions, String taskId);
}
