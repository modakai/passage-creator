package com.sakura.passage_creator.dict.model.vo;

import com.sakura.passage_creator.dict.model.entity.DictItem;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典明细返回对象
 *
 * @author sakura
 */
@Data
@AutoMapper(target = DictItem.class)
public class DictItemVO implements Serializable {

    /**
     * 主键 id
     */
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
     * 状态
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

    private static final long serialVersionUID = 1L;
}
