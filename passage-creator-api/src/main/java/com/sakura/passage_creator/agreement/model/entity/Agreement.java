package com.sakura.passage_creator.agreement.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协议内容实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_agreement")
public class Agreement implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 协议类型编码。
     */
    private String agreementType;

    /**
     * 协议标题。
     */
    private String title;

    /**
     * 协议富文本 HTML 内容。
     */
    private String content;

    /**
     * 状态：1 启用，0 禁用。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
