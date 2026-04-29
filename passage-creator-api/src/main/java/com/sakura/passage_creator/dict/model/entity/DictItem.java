package com.sakura.passage_creator.dict.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典明细
 *
 * @author sakura
 */
@Data
@Table("sys_dict_item")
public class DictItem implements Serializable {

    /**
     * 主键 id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 字典类型 id
     */
    private Long dictTypeId;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 字典值
     */
    private String dictValue;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 标签类型
     */
    private String tagType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展 JSON
     */
    private String extJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    /**
     * 序列化版本号
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
