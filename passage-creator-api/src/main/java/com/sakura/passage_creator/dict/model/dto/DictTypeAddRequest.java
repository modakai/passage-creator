package com.sakura.passage_creator.dict.model.dto;

import com.sakura.passage_creator.dict.model.entity.DictType;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增字典类型请求
 *
 * @author sakura
 */
@Data
@AutoMapper(target = DictType.class, reverseConvertGenerate = false)
public class DictTypeAddRequest implements Serializable {

    /**
     * 字典编码
     */
    @NotBlank(message = "{validation.dict.code.not_blank}")
    private String dictCode;

    /**
     * 字典名称
     */
    @NotBlank(message = "{validation.dict.name.not_blank}")
    private String dictName;

    /**
     * 状态
     */
    @NotNull(message = "{validation.dict.status.not_null}")
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
