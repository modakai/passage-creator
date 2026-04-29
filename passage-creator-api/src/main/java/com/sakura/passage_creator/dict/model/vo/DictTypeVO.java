package com.sakura.passage_creator.dict.model.vo;

import com.sakura.passage_creator.dict.model.entity.DictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典类型返回对象
 *
 * @author sakura
 */
@Data
@AutoMapper(target = DictType.class)
public class DictTypeVO implements Serializable {

    /**
     * 主键 id
     */
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
     * 状态
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

    private static final long serialVersionUID = 1L;
}
