package com.sakura.passage_creator.shared.common;

import com.sakura.passage_creator.shared.constant.CommonConstant;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求
 *
 * @author sakura
 * @from sakura
 */
@Data
public class PageRequest {

    /**
     * 当前页号，前后端统一使用 page 命名。
     */
    @Min(value = 1, message = "当前页号必须大于等于 1")
    private int page = 1;

    /**
     * 页面大小
     */
    @Min(value = 1, message = "页面大小必须大于等于 1")
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序，默认升序
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    /**
     * 兼容旧字段 current，避免存量请求立即失效。
     *
     * @return 当前页号
     */
    public int getCurrent() {
        return page;
    }

    /**
     * 兼容旧字段 current，统一写回 page。
     *
     * @param current 当前页号
     */
    public void setCurrent(int current) {
        this.page = current;
    }
}
