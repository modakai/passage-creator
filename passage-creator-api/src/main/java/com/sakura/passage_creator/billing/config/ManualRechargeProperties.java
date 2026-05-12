package com.sakura.passage_creator.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 人工扫码充值配置，统一管理内测套餐和收款码资源地址。
 */
@Data
@Component
@ConfigurationProperties(prefix = "billing.manual-recharge")
public class ManualRechargeProperties {

    /**
     * 是否启用人工充值入口。
     */
    private boolean enabled = true;

    /**
     * 微信收款码图片地址，允许部署环境改为对象存储或静态资源路径。
     */
    private String wechatQrCodeUrl = "/api/manual-recharge/wechat-qr.svg";

    /**
     * 支付宝收款码图片地址，允许部署环境改为对象存储或静态资源路径。
     */
    private String alipayQrCodeUrl = "/api/manual-recharge/alipay-qr.svg";

    /**
     * 可购买积分套餐列表，金额和积分以这里为唯一可信源。
     */
    private List<PackageConfig> packages = new ArrayList<>();

    /**
     * 人工充值套餐配置。
     */
    @Data
    public static class PackageConfig {

        /**
         * 套餐 ID，前端创建申请时只能提交该字段。
         */
        private String packageId;

        /**
         * 套餐展示名称。
         */
        private String name;

        /**
         * 用户应支付金额，单位元。
         */
        private BigDecimal amount;

        /**
         * 管理员审核通过后应发积分。
         */
        private BigDecimal credits;

        /**
         * 是否启用该套餐。
         */
        private boolean enabled = true;

        /**
         * 展示排序，数值越小越靠前。
         */
        private int sortOrder;
    }
}
