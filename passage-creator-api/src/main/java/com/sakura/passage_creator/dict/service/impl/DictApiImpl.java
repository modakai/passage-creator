package com.sakura.passage_creator.dict.service.impl;

import com.sakura.passage_creator.dict.api.DictApi;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.service.DictItemService;
import com.sakura.passage_creator.dict.service.DictTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 字典模块对外 API 实现，屏蔽内部字典实体和服务结构。
 */
@Service
public class DictApiImpl implements DictApi {

    /**
     * 字典类型服务。
     */
    private final DictTypeService dictTypeService;

    /**
     * 字典项服务。
     */
    private final DictItemService dictItemService;

    public DictApiImpl(DictTypeService dictTypeService, DictItemService dictItemService) {
        this.dictTypeService = dictTypeService;
        this.dictItemService = dictItemService;
    }

    @Override
    public boolean existsEnabledValue(String dictCode, String dictValue) {
        if (StringUtils.isAnyBlank(dictCode, dictValue)) {
            return false;
        }
        DictType dictType = dictTypeService.getByDictCode(dictCode);
        if (dictType == null) {
            return false;
        }
        return dictItemService.listEnabledByTypeId(dictType.getId()).stream()
                .map(DictItem::getDictValue)
                .anyMatch(value -> StringUtils.equals(value, dictValue));
    }
}
