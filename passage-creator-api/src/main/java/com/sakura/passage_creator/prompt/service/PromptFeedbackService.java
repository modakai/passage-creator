package com.sakura.passage_creator.prompt.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackSubmitRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptFeedback;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackStatsVO;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackVO;
import com.sakura.passage_creator.shared.context.LoginUserInfo;

import java.util.List;

/**
 * Prompt 反馈服务。
 */
public interface PromptFeedbackService extends IService<PromptFeedback> {

    /**
     * 提交或更新当前用户在指定任务环节的最终反馈。
     */
    PromptFeedbackVO submitFeedback(PromptFeedbackSubmitRequest request, LoginUserInfo loginUser);

    /**
     * 构造管理端查询条件。
     */
    QueryWrapper getQueryWrapper(PromptFeedbackQueryRequest request);

    /**
     * 按已提交反馈统计四档满意度占比。
     */
    List<PromptFeedbackStatsVO> listStats(PromptFeedbackQueryRequest request);

    /**
     * 转换单条反馈记录。
     */
    PromptFeedbackVO getFeedbackVO(PromptFeedback feedback);

    /**
     * 转换反馈记录列表。
     */
    List<PromptFeedbackVO> getFeedbackVO(List<PromptFeedback> feedbackList);
}
