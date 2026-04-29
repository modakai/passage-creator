package com.sakura.passage_creator.audit.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.audit.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper。
 *
 * @author Sakura
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
