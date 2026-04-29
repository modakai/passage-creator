package com.sakura.passage_creator.system.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置视图。
 */
@Data
public class SystemConfigVO implements Serializable {

    /**
     * 配置键。
     */
    private String key;

    /**
     * 配置值。
     */
    private String value;

    /**
     * 配置说明。
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
