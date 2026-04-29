package com.sakura.passage_creator.agreement.model.dto;

import com.sakura.passage_creator.agreement.model.entity.Agreement;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增协议请求。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = Agreement.class, reverseConvertGenerate = false)
public class AgreementAddRequest implements Serializable {

    /**
     * 协议类型编码。
     */
    @NotBlank(message = "{validation.agreement.type.not_blank}")
    private String agreementType;

    /**
     * 协议标题。
     */
    @NotBlank(message = "{validation.agreement.title.not_blank}")
    private String title;

    /**
     * 协议富文本 HTML 内容。
     */
    @NotBlank(message = "{validation.agreement.content.not_blank}")
    private String content;

    /**
     * 状态：1 启用，0 禁用。
     */
    @Min(value = 0, message = "{validation.agreement.status.invalid}")
    @Max(value = 1, message = "{validation.agreement.status.invalid}")
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 备注。
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
