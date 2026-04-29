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
 * 字典类型
 *
 * @author sakura
 */
@Data
@Table("sys_dict_type")
public class DictType implements Serializable {

    /**
     * 主键 id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 字典编码
     */
    private String dictCode;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

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
