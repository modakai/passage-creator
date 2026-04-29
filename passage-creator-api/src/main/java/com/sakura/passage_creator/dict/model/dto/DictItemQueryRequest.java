package com.sakura.passage_creator.dict.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 字典明细分页查询请求
 *
 * @author sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DictItemQueryRequest extends PageRequest implements Serializable {

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
     * 状态
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
