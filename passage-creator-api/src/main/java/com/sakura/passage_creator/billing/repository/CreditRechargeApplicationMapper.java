package com.sakura.passage_creator.billing.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.billing.model.entity.CreditRechargeApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人工扫码充值申请 Mapper。
 */
@Mapper
public interface CreditRechargeApplicationMapper extends BaseMapper<CreditRechargeApplication> {
}
