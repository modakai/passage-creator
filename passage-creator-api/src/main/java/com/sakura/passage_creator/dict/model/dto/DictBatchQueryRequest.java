package com.sakura.passage_creator.dict.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 字典批量查询请求
 *
 * @author sakura
 */
@Data
public class DictBatchQueryRequest implements Serializable {

    /**
     * 字典编码列表
     */
    @NotEmpty(message = "字典编码列表不能为空")
    private List<@NotBlank(message = "字典编码不能为空") String> dictCodes;

    private static final long serialVersionUID = 1L;
}
