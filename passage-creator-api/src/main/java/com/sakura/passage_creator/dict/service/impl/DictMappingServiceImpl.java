package com.sakura.passage_creator.dict.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.model.vo.DictItemSimpleVO;
import com.sakura.passage_creator.dict.service.DictItemService;
import com.sakura.passage_creator.dict.service.DictMappingService;
import com.sakura.passage_creator.dict.service.DictTypeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典映射服务实现
 *
 * @author sakura
 */
@Service
public class DictMappingServiceImpl implements DictMappingService {

    @Resource
    private DictTypeService dictTypeService;

    @Resource
    private DictItemService dictItemService;

    @Override
    public List<DictItemSimpleVO> getEnabledItemsByCode(String dictCode) {
        if (StringUtils.isBlank(dictCode)) {
            return new ArrayList<>();
        }
        DictType dictType = dictTypeService.getByDictCode(dictCode);
        if (dictType == null || dictType.getStatus() == null || dictType.getStatus() != 1) {
            return new ArrayList<>();
        }
        return dictItemService.listEnabledByTypeId(dictType.getId()).stream()
                .sorted(Comparator.comparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()))
                .map(this::toSimpleVO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<DictItemSimpleVO>> getEnabledItemMap(List<String> dictCodes) {
        Map<String, List<DictItemSimpleVO>> resultMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(dictCodes)) {
            return resultMap;
        }
        for (String dictCode : dictCodes) {
            resultMap.put(dictCode, getEnabledItemsByCode(dictCode));
        }
        return resultMap;
    }

    @Override
    public String getLabelByCodeAndValue(String dictCode, String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        for (DictItemSimpleVO item : getEnabledItemsByCode(dictCode)) {
            if (value.equals(item.getValue())) {
                return item.getLabel();
            }
        }
        return "";
    }

    /**
     * 转换为最小字典映射对象
     *
     * @param dictItem 字典明细
     * @return 映射对象
     */
    private DictItemSimpleVO toSimpleVO(DictItem dictItem) {
        DictItemSimpleVO simpleVO = new DictItemSimpleVO();
        simpleVO.setLabel(dictItem.getDictLabel());
        simpleVO.setValue(dictItem.getDictValue());
        simpleVO.setTagType(dictItem.getTagType());
        simpleVO.setSortOrder(dictItem.getSortOrder());
        simpleVO.setExtJson(dictItem.getExtJson());
        return simpleVO;
    }
}
