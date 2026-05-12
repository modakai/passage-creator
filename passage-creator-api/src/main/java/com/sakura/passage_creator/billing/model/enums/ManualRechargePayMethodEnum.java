package com.sakura.passage_creator.billing.model.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 人工充值付款方式枚举。
 */
@Getter
public enum ManualRechargePayMethodEnum {

    /**
     * 微信收款码。
     */
    WECHAT("WECHAT", "微信"),

    /**
     * 支付宝收款码。
     */
    ALIPAY("ALIPAY", "支付宝"),

    /**
     * 未知或用户未选择。
     */
    UNKNOWN("UNKNOWN", "未知");

    /**
     * 持久化值。
     */
    private final String value;

    /**
     * 展示文案。
     */
    private final String label;

    ManualRechargePayMethodEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 解析付款方式，非法值统一降级为 UNKNOWN。
     */
    public static ManualRechargePayMethodEnum of(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
