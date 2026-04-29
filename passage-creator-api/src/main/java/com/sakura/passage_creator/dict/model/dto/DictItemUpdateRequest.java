package com.sakura.passage_creator.dict.model.dto;

import com.sakura.passage_creator.dict.model.entity.DictItem;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新字典明细请求
 *
 * @author sakura
 */
@Data
@AutoMapper(target = DictItem.class, reverseConvertGenerate = false)
public class DictItemUpdateRequest implements Serializable {

    /**
     * 主键 id
     */
    @NotNull(message = "{validation.dict.item_id.not_null}")
    @Positive(message = "字典明细 id 必须大于 0")
    private Long id;

    /**
     * 字典类型 id
     */
    @NotNull(message = "{validation.dict.type_id.not_null}")
    @Positive(message = "字典类型 id 必须大于 0")
    private Long dictTypeId;

    /**
     * 字典标签
     */
    @NotBlank(message = "{validation.dict.label.not_blank}")
    private String dictLabel;

    /**
     * 字典值
     */
    @NotBlank(message = "{validation.dict.value.not_blank}")
    private String dictValue;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 状态
     */
    @NotNull(message = "{validation.dict.status.not_null}")
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

    private static final long serialVersionUID = 1L;
}
