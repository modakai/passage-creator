package com.sakura.passage_creator.billing.model.enums;

import lombok.Getter;

/**
 * 积分流水类型，所有余额变化都必须落一条流水。
 */
@Getter
public enum CreditTransactionTypeEnum {

    RECHARGE("充值", "RECHARGE"),
    RESERVE("预扣", "RESERVE"),
    CONSUME("消费", "CONSUME"),
    REFUND("退款", "REFUND"),
    ADJUST("人工调整", "ADJUST");

    private final String text;

    private final String value;

    CreditTransactionTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
