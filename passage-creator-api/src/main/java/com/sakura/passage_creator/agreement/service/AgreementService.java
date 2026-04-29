package com.sakura.passage_creator.agreement.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.agreement.model.dto.AgreementAddRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementQueryRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementUpdateRequest;
import com.sakura.passage_creator.agreement.model.entity.Agreement;
import com.sakura.passage_creator.agreement.model.vo.AgreementVO;

import java.util.List;

/**
 * 协议内容服务。
 *
 * @author Sakura
 */
public interface AgreementService extends IService<Agreement> {

    /**
     * 新增协议。
     *
     * @param request 新增请求
     * @return 新增记录 id
     */
    Long addAgreement(AgreementAddRequest request);

    /**
     * 更新协议。
     *
     * @param request 更新请求
     * @return 是否成功
     */
    boolean updateAgreement(AgreementUpdateRequest request);

    /**
     * 删除协议。
     *
     * @param id 协议 id
     * @return 是否成功
     */
    boolean removeAgreement(Long id);

    /**
     * 构造分页查询条件。
     *
     * @param queryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AgreementQueryRequest queryRequest);

    /**
     * 判断协议类型是否已存在。
     *
     * @param agreementType 协议类型编码
     * @param excludeId 排除的记录 id
     * @return 是否存在
     */
    boolean existsByAgreementType(String agreementType, Long excludeId);

    /**
     * 根据协议类型获取协议内容。
     *
     * @param agreementType 协议类型编码
     * @return 协议内容
     */
    Agreement getByAgreementType(String agreementType);

    /**
     * 获取启用中的协议内容。
     *
     * @param agreementType 协议类型编码
     * @return 协议内容
     */
    Agreement getEnabledAgreementByType(String agreementType);

    /**
     * 将实体转换为返回对象。
     *
     * @param agreement 协议实体
     * @return 返回对象
     */
    AgreementVO getAgreementVO(Agreement agreement);

    /**
     * 将实体列表转换为返回对象列表。
     *
     * @param agreementList 协议实体列表
     * @return 返回对象列表
     */
    List<AgreementVO> getAgreementVO(List<Agreement> agreementList);
}
