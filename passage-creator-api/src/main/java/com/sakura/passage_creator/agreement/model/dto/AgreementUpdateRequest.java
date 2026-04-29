package com.sakura.passage_creator.agreement.model.dto;

import com.sakura.passage_creator.agreement.model.entity.Agreement;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 更新协议请求。
 *
 * @author Sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AutoMapper(target = Agreement.class, reverseConvertGenerate = false)
public class AgreementUpdateRequest extends AgreementAddRequest implements Serializable {

    /**
     * 协议 id。
     */
    @NotNull(message = "{validation.agreement.id.not_null}")
    @Positive(message = "{validation.agreement.id.positive}")
    private Long id;

    private static final long serialVersionUID = 1L;
}
