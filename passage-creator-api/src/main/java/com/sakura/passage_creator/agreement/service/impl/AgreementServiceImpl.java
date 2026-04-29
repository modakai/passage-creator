package com.sakura.passage_creator.agreement.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.agreement.model.dto.AgreementAddRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementQueryRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementUpdateRequest;
import com.sakura.passage_creator.agreement.model.entity.Agreement;
import com.sakura.passage_creator.agreement.model.vo.AgreementVO;
import com.sakura.passage_creator.agreement.repository.AgreementMapper;
import com.sakura.passage_creator.agreement.service.AgreementService;
import com.sakura.passage_creator.dict.api.DictApi;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.agreement.model.entity.table.AgreementTableDef.AGREEMENT;

/**
 * 协议内容服务实现。
 *
 * @author Sakura
 */
@Service
public class AgreementServiceImpl extends ServiceImpl<AgreementMapper, Agreement> implements AgreementService {

    /**
     * 协议类型字典编码。
     */
    private static final String AGREEMENT_TYPE_DICT_CODE = "agreement";

    /**
     * 字典模块 API，用于校验协议类型是否为启用字典值。
     */
    private final DictApi dictApi;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    private final Converter converter;

    public AgreementServiceImpl(DictApi dictApi, Converter converter) {
        this.dictApi = dictApi;
        this.converter = converter;
    }

    @Override
    public Long addAgreement(AgreementAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Agreement agreement = converter.convert(request, Agreement.class);
        validAgreement(agreement, true);
        boolean result = this.save(agreement);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return agreement.getId();
    }

    @Override
    public boolean updateAgreement(AgreementUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        assertAgreementExists(request.getId());
        Agreement agreement = converter.convert(request, Agreement.class);
        validAgreement(agreement, false);
        return this.updateById(agreement);
    }

    @Override
    public boolean removeAgreement(Long id) {
        assertAgreementExists(id);
        return this.removeById(id);
    }

    @Override
    public QueryWrapper getQueryWrapper(AgreementQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.param.null");
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AGREEMENT.ID.eq(queryRequest.getId(), queryRequest.getId() != null));
        queryWrapper.and(AGREEMENT.AGREEMENT_TYPE.eq(queryRequest.getAgreementType(),
                StringUtils.isNotBlank(queryRequest.getAgreementType())));
        queryWrapper.and(AGREEMENT.TITLE.like(queryRequest.getTitle(), StringUtils.isNotBlank(queryRequest.getTitle())));
        queryWrapper.and(AGREEMENT.STATUS.eq(queryRequest.getStatus(), queryRequest.getStatus() != null));
        queryWrapper.orderBy(AGREEMENT.SORT_ORDER, true);
        queryWrapper.orderBy(AGREEMENT.ID, false);
        QueryColumn sortColumn = resolveSortColumn(queryRequest.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(queryRequest.getSortOrder()));
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为协议表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> AGREEMENT.ID;
            case "agreement_type" -> AGREEMENT.AGREEMENT_TYPE;
            case "title" -> AGREEMENT.TITLE;
            case "status" -> AGREEMENT.STATUS;
            case "sort_order" -> AGREEMENT.SORT_ORDER;
            case "create_time" -> AGREEMENT.CREATE_TIME;
            case "update_time" -> AGREEMENT.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public boolean existsByAgreementType(String agreementType, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AGREEMENT.AGREEMENT_TYPE.eq(agreementType));
        if (excludeId != null) {
            queryWrapper.and(AGREEMENT.ID.ne(excludeId));
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Agreement getByAgreementType(String agreementType) {
        if (StringUtils.isBlank(agreementType)) {
            return null;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AGREEMENT.AGREEMENT_TYPE.eq(agreementType));
        return this.getOne(queryWrapper);
    }

    @Override
    public Agreement getEnabledAgreementByType(String agreementType) {
        Agreement agreement = getByAgreementType(agreementType);
        ThrowUtils.throwIf(agreement == null, ErrorCode.NOT_FOUND_ERROR, "agreement.not_found");
        ThrowUtils.throwIf(agreement.getStatus() == null || agreement.getStatus() != 1,
                ErrorCode.NOT_FOUND_ERROR, "agreement.not_found");
        return agreement;
    }

    @Override
    public AgreementVO getAgreementVO(Agreement agreement) {
        if (agreement == null) {
            return null;
        }
        return converter.convert(agreement, AgreementVO.class);
    }

    @Override
    public List<AgreementVO> getAgreementVO(List<Agreement> agreementList) {
        if (CollUtil.isEmpty(agreementList)) {
            return new ArrayList<>();
        }
        return agreementList.stream().map(this::getAgreementVO).collect(Collectors.toList());
    }

    /**
     * 断言协议记录存在。
     *
     * @param id 协议 id
     */
    public void assertAgreementExists(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Agreement oldAgreement = this.getById(id);
        ThrowUtils.throwIf(oldAgreement == null, ErrorCode.NOT_FOUND_ERROR);
    }

    /**
     * 校验协议内容参数。
     *
     * @param agreement 协议实体
     * @param add 是否新增
     */
    private void validAgreement(Agreement agreement, boolean add) {
        if (agreement == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!add && (agreement.getId() == null || agreement.getId() <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.id.invalid");
        }
        if (StringUtils.isBlank(agreement.getAgreementType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.type.blank");
        }
        if (StringUtils.isBlank(agreement.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.title.blank");
        }
        if (StringUtils.isBlank(agreement.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.content.blank");
        }
        if (agreement.getStatus() == null || (agreement.getStatus() != 0 && agreement.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "agreement.status.invalid");
        }
        if (agreement.getSortOrder() == null) {
            agreement.setSortOrder(0);
        }
        validateAgreementType(agreement.getAgreementType());
        boolean exists = existsByAgreementType(agreement.getAgreementType(), add ? null : agreement.getId());
        ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "agreement.exists");
    }

    /**
     * 校验协议类型是否存在于字典配置中。
     *
     * @param agreementType 协议类型编码
     */
    private void validateAgreementType(String agreementType) {
        boolean exists = dictApi.existsEnabledValue(AGREEMENT_TYPE_DICT_CODE, agreementType);
        ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "agreement.type.not_found");
    }
}
