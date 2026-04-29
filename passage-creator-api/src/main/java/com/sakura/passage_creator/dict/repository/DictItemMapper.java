package com.sakura.passage_creator.dict.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典明细 Mapper
 *
 * @author sakura
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
}
