package com.sakura.passage_creator.dict.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.dict.model.dto.DictTypeAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeUpdateRequest;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.model.vo.DictTypeVO;
import com.sakura.passage_creator.dict.repository.DictTypeMapper;
import com.sakura.passage_creator.dict.service.DictItemService;
import com.sakura.passage_creator.dict.service.DictTypeService;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.dict.model.entity.table.DictTypeTableDef.DICT_TYPE;

/**
 * 字典类型服务实现
 *
 * @author sakura
 */
@Service
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, DictType> implements DictTypeService {

    /**
     * 字典编码合法格式
     */
    private static final Pattern DICT_CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    @Resource
    private DictItemService dictItemService;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    @Override
    public Long addDictType(DictTypeAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DictType dictType = converter.convert(request, DictType.class);
        validDictType(dictType, true);
        boolean result = this.save(dictType);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return dictType.getId();
    }

    @Override
    public boolean updateDictType(DictTypeUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DictType oldDictType = this.getById(request.getId());
        ThrowUtils.throwIf(oldDictType == null, ErrorCode.NOT_FOUND_ERROR);
        DictType dictType = converter.convert(request, DictType.class);
        validDictType(dictType, false);
        return this.updateById(dictType);
    }

    @Override
    public boolean removeDictType(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long itemCount = dictItemService.countEnabledByTypeId(id);
        ThrowUtils.throwIf(itemCount > 0, ErrorCode.OPERATION_ERROR, "请先删除该字典类型下的明细数据");
        DictType oldDictType = this.getById(id);
        ThrowUtils.throwIf(oldDictType == null, ErrorCode.NOT_FOUND_ERROR);
        return this.removeById(id);
    }

    @Override
    public QueryWrapper getQueryWrapper(DictTypeQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(DICT_TYPE.ID.eq(queryRequest.getId(), queryRequest.getId() != null));
        queryWrapper.and(DICT_TYPE.DICT_CODE.like(queryRequest.getDictCode(),
                StringUtils.isNotBlank(queryRequest.getDictCode())));
        queryWrapper.and(DICT_TYPE.DICT_NAME.like(queryRequest.getDictName(),
                StringUtils.isNotBlank(queryRequest.getDictName())));
        queryWrapper.and(DICT_TYPE.STATUS.eq(queryRequest.getStatus(), queryRequest.getStatus() != null));
        QueryColumn sortColumn = resolveSortColumn(queryRequest.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(queryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy(DICT_TYPE.ID, false);
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为字典类型表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> DICT_TYPE.ID;
            case "dict_code" -> DICT_TYPE.DICT_CODE;
            case "dict_name" -> DICT_TYPE.DICT_NAME;
            case "status" -> DICT_TYPE.STATUS;
            case "create_time" -> DICT_TYPE.CREATE_TIME;
            case "update_time" -> DICT_TYPE.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public boolean existsByDictCode(String dictCode, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DICT_TYPE.DICT_CODE.eq(dictCode));
        if (excludeId != null) {
            queryWrapper.and(DICT_TYPE.ID.ne(excludeId));
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public DictType getByDictCode(String dictCode) {
        if (StringUtils.isBlank(dictCode)) {
            return null;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DICT_TYPE.DICT_CODE.eq(dictCode));
        return this.getOne(queryWrapper);
    }

    @Override
    public DictTypeVO getDictTypeVO(DictType dictType) {
        if (dictType == null) {
            return null;
        }
        return converter.convert(dictType, DictTypeVO.class);
    }

    @Override
    public List<DictTypeVO> getDictTypeVO(List<DictType> dictTypeList) {
        if (CollUtil.isEmpty(dictTypeList)) {
            return new ArrayList<>();
        }
        return dictTypeList.stream().map(this::getDictTypeVO).collect(Collectors.toList());
    }

    /**
     * 校验字典类型参数
     *
     * @param dictType 字典类型实体
     * @param add 是否新增
     */
    private void validDictType(DictType dictType, boolean add) {
        if (dictType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!add && (dictType.getId() == null || dictType.getId() <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型 id 非法");
        }
        if (StringUtils.isBlank(dictType.getDictCode()) || !DICT_CODE_PATTERN.matcher(dictType.getDictCode()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典编码格式错误，仅支持字母、数字和下划线，且必须以字母开头");
        }
        if (StringUtils.isBlank(dictType.getDictName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典名称不能为空");
        }
        if (dictType.getStatus() == null || (dictType.getStatus() != 0 && dictType.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典状态非法");
        }
        boolean exists = existsByDictCode(dictType.getDictCode(), add ? null : dictType.getId());
        ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "字典编码已存在");
    }
}
