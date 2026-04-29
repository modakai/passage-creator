package com.sakura.passage_creator.article.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.article.model.dto.ArticleAddRequest;
import com.sakura.passage_creator.article.model.dto.ArticleQueryRequest;
import com.sakura.passage_creator.article.model.dto.ArticleUpdateRequest;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.vo.ArticleVO;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章管理接口。
 */
@RestController
@RequestMapping("/article")
@Validated
public class ArticleController {

    @Resource
    private ArticleService articleService;

    /**
     * 新增文章。
     *
     * @param request 新增请求
     * @param httpServletRequest 当前请求
     * @return 新增文章 id
     */
    @PostMapping("/add")
    @AuditLogRecord(description = "新增文章", module = "文章管理", operationType = AuditOperationTypeEnum.CREATE)
    public BaseResponse<Long> addArticle(@Valid @RequestBody ArticleAddRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(articleService.addArticle(request, getLoginUser()));
    }

    /**
     * 更新文章。
     *
     * @param request 更新请求
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuditLogRecord(description = "更新文章", module = "文章管理", operationType = AuditOperationTypeEnum.UPDATE)
    public BaseResponse<Boolean> updateArticle(@Valid @RequestBody ArticleUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(articleService.updateArticle(request, getLoginUser()));
    }

    /**
     * 删除文章。
     *
     * @param deleteRequest 删除请求
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuditLogRecord(description = "删除文章", module = "文章管理", operationType = AuditOperationTypeEnum.DELETE)
    public BaseResponse<Boolean> deleteArticle(@Valid @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(articleService.removeArticle(deleteRequest.getId(), getLoginUser()));
    }

    /**
     * 根据 id 获取文章详情。
     *
     * @param id 文章 id
     * @param httpServletRequest 当前请求
     * @return 文章详情
     */
    @GetMapping("/get")
    public BaseResponse<ArticleVO> getArticleById(@RequestParam @Positive(message = "文章 id 必须大于 0") long id,
            HttpServletRequest httpServletRequest) {
        Article article = articleService.getAccessibleArticle(id, getLoginUser());
        return ResultUtils.success(articleService.getArticleVO(article));
    }

    /**
     * 分页获取文章列表。
     *
     * @param queryRequest 查询请求
     * @param httpServletRequest 当前请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ArticleVO>> listArticleByPage(@Valid @RequestBody ArticleQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        LoginUserInfo loginUser = getLoginUser();
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        Page<Article> page = articleService.page(new Page<>(current, pageSize),
                articleService.getQueryWrapper(queryRequest, loginUser));
        List<ArticleVO> articleVOList = articleService.getArticleVO(page.getRecords());
        Page<ArticleVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(articleVOList);
        return ResultUtils.success(voPage);
    }

    /**
     * 读取当前请求登录用户。
     */
    private LoginUserInfo getLoginUser() {
        return LoginUserContext.getLoginUser();
    }
}
