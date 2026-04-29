package com.sakura.passage_creator.dict.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.dict.model.dto.DictItemAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemUpdateRequest;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.model.vo.DictItemVO;
import com.sakura.passage_creator.dict.repository.DictItemMapper;
import com.sakura.passage_creator.dict.repository.DictTypeMapper;
import com.sakura.passage_creator.dict.service.DictItemService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.dict.model.entity.table.DictItemTableDef.DICT_ITEM;

/**
 * 字典明细服务实现
 *
 * @author sakura
 */
@Service
public class DictItemServiceImpl extends ServiceImpl<DictItemMapper, DictItem> implements DictItemService {

    @Resource
    private DictTypeMapper dictTypeMapper;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    @Override
    public Long addDictItem(DictItemAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DictItem dictItem = converter.convert(request, DictItem.class);
        validDictItem(dictItem, true);
        boolean result = this.save(dictItem);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return dictItem.getId();
    }

    @Override
    public boolean updateDictItem(DictItemUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DictItem oldDictItem = this.getById(request.getId());
        ThrowUtils.throwIf(oldDictItem == null, ErrorCode.NOT_FOUND_ERROR);
        DictItem dictItem = converter.convert(request, DictItem.class);
        validDictItem(dictItem, false);
        return this.updateById(dictItem);
    }

    @Override
    public boolean removeDictItem(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DictItem oldDictItem = this.getById(id);
        ThrowUtils.throwIf(oldDictItem == null, ErrorCode.NOT_FOUND_ERROR);
        return this.removeById(id);
    }

    @Override
    public QueryWrapper getQueryWrapper(DictItemQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(DICT_ITEM.ID.eq(queryRequest.getId(), queryRequest.getId() != null));
        queryWrapper.and(DICT_ITEM.DICT_TYPE_ID.eq(queryRequest.getDictTypeId(), queryRequest.getDictTypeId() != null));
        queryWrapper.and(DICT_ITEM.DICT_LABEL.like(queryRequest.getDictLabel(),
                StringUtils.isNotBlank(queryRequest.getDictLabel())));
        queryWrapper.and(DICT_ITEM.DICT_VALUE.like(queryRequest.getDictValue(),
                StringUtils.isNotBlank(queryRequest.getDictValue())));
        queryWrapper.and(DICT_ITEM.STATUS.eq(queryRequest.getStatus(), queryRequest.getStatus() != null));
        queryWrapper.orderBy(DICT_ITEM.SORT_ORDER, true);
        queryWrapper.orderBy(DICT_ITEM.ID, false);
        QueryColumn sortColumn = resolveSortColumn(queryRequest.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(queryRequest.getSortOrder()));
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为字典明细表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> DICT_ITEM.ID;
            case "dict_type_id" -> DICT_ITEM.DICT_TYPE_ID;
            case "dict_label" -> DICT_ITEM.DICT_LABEL;
            case "dict_value" -> DICT_ITEM.DICT_VALUE;
            case "status" -> DICT_ITEM.STATUS;
            case "sort_order" -> DICT_ITEM.SORT_ORDER;
            case "create_time" -> DICT_ITEM.CREATE_TIME;
            case "update_time" -> DICT_ITEM.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public long countEnabledByTypeId(Long dictTypeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DICT_ITEM.DICT_TYPE_ID.eq(dictTypeId))
                .and(DICT_ITEM.IS_DELETE.eq(0));
        return this.count(queryWrapper);
    }

    @Override
    public List<DictItem> listEnabledByTypeId(Long dictTypeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DICT_ITEM.DICT_TYPE_ID.eq(dictTypeId))
                .and(DICT_ITEM.STATUS.eq(1))
                .and(DICT_ITEM.IS_DELETE.eq(0))
                .orderBy(DICT_ITEM.SORT_ORDER, true)
                .orderBy(DICT_ITEM.ID, false);
        return this.list(queryWrapper);
    }

    @Override
    public boolean existsByTypeAndValue(Long dictTypeId, String dictValue, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DICT_ITEM.DICT_TYPE_ID.eq(dictTypeId))
                .and(DICT_ITEM.DICT_VALUE.eq(dictValue));
        if (excludeId != null) {
            queryWrapper.and(DICT_ITEM.ID.ne(excludeId));
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public DictItemVO getDictItemVO(DictItem dictItem) {
        if (dictItem == null) {
            return null;
        }
        return converter.convert(dictItem, DictItemVO.class);
    }

    @Override
    public List<DictItemVO> getDictItemVO(List<DictItem> dictItemList) {
        if (CollUtil.isEmpty(dictItemList)) {
            return new ArrayList<>();
        }
        return dictItemList.stream().map(this::getDictItemVO).collect(Collectors.toList());
    }

    /**
     * 校验字典明细参数
     *
     * @param dictItem 字典明细
     * @param add 是否新增
     */
    private void validDictItem(DictItem dictItem, boolean add) {
        if (dictItem == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!add && (dictItem.getId() == null || dictItem.getId() <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典明细 id 非法");
        }
        if (dictItem.getDictTypeId() == null || dictItem.getDictTypeId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型不存在");
        }
        DictType dictType = dictTypeMapper.selectOneById(dictItem.getDictTypeId());
        ThrowUtils.throwIf(dictType == null, ErrorCode.NOT_FOUND_ERROR, "字典类型不存在");
        if (StringUtils.isBlank(dictItem.getDictLabel())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典标签不能为空");
        }
        if (StringUtils.isBlank(dictItem.getDictValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典值不能为空");
        }
        if (dictItem.getStatus() == null || (dictItem.getStatus() != 0 && dictItem.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典状态非法");
        }
        if (dictItem.getSortOrder() == null) {
            dictItem.setSortOrder(0);
        }
        boolean exists = existsByTypeAndValue(dictItem.getDictTypeId(), dictItem.getDictValue(), add ? null : dictItem.getId());
        ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "同一字典类型下的字典值已存在");
    }
}
