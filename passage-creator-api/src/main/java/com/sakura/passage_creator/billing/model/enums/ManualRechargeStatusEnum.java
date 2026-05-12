package com.sakura.passage_creator.billing.model.enums;

import lombok.Getter;

/**
 * 人工充值申请状态枚举。
 */
@Getter
public enum ManualRechargeStatusEnum {

    /**
     * 待管理员审核。
     */
    PENDING("PENDING", "待审核"),

    /**
     * 已审核通过并完成积分入账。
     */
    APPROVED("APPROVED", "已到账"),

    /**
     * 已被管理员拒绝。
     */
    REJECTED("REJECTED", "已拒绝");

    /**
     * 持久化值。
     */
    private final String value;

    /**
     * 展示文案。
     */
    private final String label;

    ManualRechargeStatusEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
