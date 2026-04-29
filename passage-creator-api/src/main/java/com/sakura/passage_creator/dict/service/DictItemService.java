package com.sakura.passage_creator.dict.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.dict.model.dto.DictItemAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemUpdateRequest;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import com.sakura.passage_creator.dict.model.vo.DictItemVO;

import java.util.List;

/**
 * 字典明细服务
 *
 * @author sakura
 */
public interface DictItemService extends IService<DictItem> {

    /**
     * 新增字典明细
     *
     * @param request 请求参数
     * @return 新增记录 id
     */
    Long addDictItem(DictItemAddRequest request);

    /**
     * 更新字典明细
     *
     * @param request 请求参数
     * @return 是否成功
     */
    boolean updateDictItem(DictItemUpdateRequest request);

    /**
     * 删除字典明细
     *
     * @param id 明细 id
     * @return 是否成功
     */
    boolean removeDictItem(Long id);

    /**
     * 构造分页查询条件
     *
     * @param queryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(DictItemQueryRequest queryRequest);

    /**
     * 获取可用明细数量
     *
     * @param dictTypeId 字典类型 id
     * @return 数量
     */
    long countEnabledByTypeId(Long dictTypeId);

    /**
     * 获取可用明细列表
     *
     * @param dictTypeId 字典类型 id
     * @return 明细列表
     */
    List<DictItem> listEnabledByTypeId(Long dictTypeId);

    /**
     * 判断同类型下字典值是否已存在
     *
     * @param dictTypeId 类型 id
     * @param dictValue 字典值
     * @param excludeId 排除 id
     * @return 是否存在
     */
    boolean existsByTypeAndValue(Long dictTypeId, String dictValue, Long excludeId);

    /**
     * 单个转 VO
     *
     * @param dictItem 字典明细
     * @return 返回对象
     */
    DictItemVO getDictItemVO(DictItem dictItem);

    /**
     * 列表转 VO
     *
     * @param dictItemList 字典明细列表
     * @return 返回对象列表
     */
    List<DictItemVO> getDictItemVO(List<DictItem> dictItemList);
}
