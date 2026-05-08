package com.sakura.passage_creator.prompt.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateAddRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateUpdateRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptTemplate;
import com.sakura.passage_creator.prompt.model.vo.PromptTemplateVO;

import java.util.List;
import java.util.Map;

/**
 * Prompt 模板版本服务。
 */
public interface PromptTemplateService extends IService<PromptTemplate> {

    /**
     * 默认运行环境。
     */
    String DEFAULT_ENVIRONMENT = "production";

    /**
     * 新增 Prompt 模板草稿。
     *
     * @param request 新增请求
     * @param operator 操作人账号
     * @return 新增模板 id
     */
    Long addTemplate(PromptTemplateAddRequest request, String operator);

    /**
     * 更新 Prompt 模板草稿。
     *
     * @param request 更新请求
     * @return 是否成功
     */
    boolean updateTemplate(PromptTemplateUpdateRequest request);

    /**
     * 发布模板版本，并归档同环境旧版本。
     *
     * @param id 模板版本 id
     * @param operator 发布人账号
     * @return 是否成功
     */
    boolean publishTemplate(Long id, String operator);

    /**
     * 归档模板版本。
     *
     * @param id 模板版本 id
     * @return 是否成功
     */
    boolean archiveTemplate(Long id);

    /**
     * 删除非 ACTIVE 模板版本。
     *
     * @param id 模板版本 id
     * @return 是否成功
     */
    boolean deleteTemplate(Long id);

    /**
     * 查询同模板同环境下已经占用的版本号。
     *
     * @param templateKey 模板标识
     * @param environment 运行环境
     * @return 已存在版本号列表
     */
    List<String> listTemplateVersions(String templateKey, String environment);

    /**
     * 刷新指定模板缓存。
     *
     * @param templateKey 模板标识
     * @param environment 运行环境
     * @return 是否成功
     */
    boolean refreshTemplate(String templateKey, String environment);

    /**
     * 渲染运行时 Prompt，未配置 ACTIVE 版本时使用兜底模板。
     *
     * @param templateKey 模板标识
     * @param fallbackContent 兜底模板内容
     * @param fallbackSchema 兜底变量定义
     * @param variables 变量值
     * @return 渲染结果
     */
    PromptTemplateRenderResult renderActive(String templateKey, String fallbackContent, String fallbackSchema,
            Map<String, ?> variables);

    /**
     * 构建分页查询条件。
     *
     * @param request 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(PromptTemplateQueryRequest request);

    /**
     * 转换为视图对象。
     *
     * @param template 模板实体
     * @return 视图对象
     */
    PromptTemplateVO getTemplateVO(PromptTemplate template);

    /**
     * 批量转换为视图对象。
     *
     * @param templateList 模板实体列表
     * @return 视图对象列表
     */
    List<PromptTemplateVO> getTemplateVO(List<PromptTemplate> templateList);
}
