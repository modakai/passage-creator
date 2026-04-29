package com.sakura.passage_creator.dict.api;

/**
 * 字典模块对外 API，供其他模块按字典编码校验启用的字典值。
 */
public interface DictApi {

    /**
     * 判断指定字典编码下是否存在启用的字典值。
     *
     * @param dictCode 字典编码
     * @param dictValue 字典值
     * @return 是否存在启用项
     */
    boolean existsEnabledValue(String dictCode, String dictValue);
}
