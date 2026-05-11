package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.billing.model.dto.AiUsageQueryRequest;
import com.sakura.passage_creator.billing.model.dto.RecordAiUsageCommand;
import com.sakura.passage_creator.billing.model.entity.AiUsageRecord;
import com.sakura.passage_creator.billing.model.vo.AiUsageRecordVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageSummaryVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageUserSummaryVO;

import java.util.List;

/**
 * AI 用量记录服务。
 */
public interface AiUsageRecordService extends IService<AiUsageRecord> {

    /**
     * 记录一次 AI 调用。
     */
    void recordUsage(RecordAiUsageCommand command);

    /**
     * 构造查询条件。
     */
    QueryWrapper getQueryWrapper(AiUsageQueryRequest request);

    /**
     * 获取全站用量总览。
     */
    AiUsageSummaryVO getSummary(AiUsageQueryRequest request);

    /**
     * 按用户聚合分页。
     */
    Page<AiUsageUserSummaryVO> listUserSummaryByPage(AiUsageQueryRequest request);

    /**
     * 转换单条记录。
     */
    AiUsageRecordVO getUsageRecordVO(AiUsageRecord record);

    /**
     * 转换记录列表。
     */
    List<AiUsageRecordVO> getUsageRecordVO(List<AiUsageRecord> records);
}
