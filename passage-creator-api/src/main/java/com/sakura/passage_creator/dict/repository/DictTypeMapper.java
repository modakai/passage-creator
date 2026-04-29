package com.sakura.passage_creator.dict.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.dict.model.entity.DictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型 Mapper
 *
 * @author sakura
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {
}
