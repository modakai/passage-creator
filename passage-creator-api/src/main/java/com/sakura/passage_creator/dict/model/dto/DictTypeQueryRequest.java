package com.sakura.passage_creator.dict.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 字典类型分页查询请求
 *
 * @author sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DictTypeQueryRequest extends PageRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
