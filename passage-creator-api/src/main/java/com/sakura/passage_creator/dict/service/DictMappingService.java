package com.sakura.passage_creator.dict.service;

import com.sakura.passage_creator.dict.model.vo.DictItemSimpleVO;

import java.util.List;
import java.util.Map;

/**
 * 字典映射服务
 *
 * @author sakura
 */
public interface DictMappingService {

    /**
     * 根据字典编码获取启用中的字典项
     *
     * @param dictCode 字典编码
     * @return 字典项列表
     */
    List<DictItemSimpleVO> getEnabledItemsByCode(String dictCode);

    /**
     * 批量获取字典映射
     *
     * @param dictCodes 字典编码列表
     * @return 字典映射
     */
    Map<String, List<DictItemSimpleVO>> getEnabledItemMap(List<String> dictCodes);

    /**
     * 根据编码和值获取标签
     *
     * @param dictCode 字典编码
     * @param value 字典值
     * @return 标签文本
     */
    String getLabelByCodeAndValue(String dictCode, String value);
}
