package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 人工充值收款信息视图。
 */
@Data
public class ManualRechargePaymentVO implements Serializable {

    /**
     * 微信收款码图片地址。
     */
    private String wechatQrCodeUrl;

    /**
     * 支付宝收款码图片地址。
     */
    private String alipayQrCodeUrl;

    /**
     * 付款备注提示文案。
     */
    private String paymentRemarkTip;

    /**
     * 人工审核提示文案。
     */
    private String auditTip;

    private static final long serialVersionUID = 1L;
}
