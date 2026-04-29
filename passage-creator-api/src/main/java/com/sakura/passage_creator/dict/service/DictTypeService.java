package com.sakura.passage_creator.dict.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.dict.model.dto.DictTypeAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeUpdateRequest;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.model.vo.DictTypeVO;

import java.util.List;

/**
 * 字典类型服务
 *
 * @author sakura
 */
public interface DictTypeService extends IService<DictType> {

    /**
     * 新增字典类型
     *
     * @param request 请求参数
     * @return 新增记录 id
     */
    Long addDictType(DictTypeAddRequest request);

    /**
     * 更新字典类型
     *
     * @param request 请求参数
     * @return 是否成功
     */
    boolean updateDictType(DictTypeUpdateRequest request);

    /**
     * 删除字典类型
     *
     * @param id 类型 id
     * @return 是否成功
     */
    boolean removeDictType(Long id);

    /**
     * 构造分页查询条件
     *
     * @param queryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(DictTypeQueryRequest queryRequest);

    /**
     * 判断字典编码是否已存在
     *
     * @param dictCode 字典编码
     * @param excludeId 排除 id
     * @return 是否存在
     */
    boolean existsByDictCode(String dictCode, Long excludeId);

    /**
     * 按编码查询字典类型
     *
     * @param dictCode 字典编码
     * @return 字典类型
     */
    DictType getByDictCode(String dictCode);

    /**
     * 单个转 VO
     *
     * @param dictType 字典类型
     * @return 返回对象
     */
    DictTypeVO getDictTypeVO(DictType dictType);

    /**
     * 列表转 VO
     *
     * @param dictTypeList 字典类型列表
     * @return 返回对象列表
     */
    List<DictTypeVO> getDictTypeVO(List<DictType> dictTypeList);
}
