package com.sakura.passage_creator.shared.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求
 *
 * @author sakura
 * @from sakura
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "{validation.common.id.not_null}")
    @Positive(message = "{validation.common.id.positive}")
    private Long id;

    private static final long serialVersionUID = 1L;
}
