package com.sakura.passage_creator.agreement.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.agreement.model.entity.Agreement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协议内容 Mapper。
 *
 * @author Sakura
 */
@Mapper
public interface AgreementMapper extends BaseMapper<Agreement> {
}
