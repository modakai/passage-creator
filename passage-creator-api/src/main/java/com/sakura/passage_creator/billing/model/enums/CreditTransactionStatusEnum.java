package com.sakura.passage_creator.billing.model.enums;

import lombok.Getter;

/**
 * 积分流水状态，预扣流水在结算或释放前保持 RESERVED。
 */
@Getter
public enum CreditTransactionStatusEnum {

    RESERVED("预扣中", "RESERVED"),
    COMPLETED("已完成", "COMPLETED"),
    RELEASED("已释放", "RELEASED");

    private final String text;

    private final String value;

    CreditTransactionStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
