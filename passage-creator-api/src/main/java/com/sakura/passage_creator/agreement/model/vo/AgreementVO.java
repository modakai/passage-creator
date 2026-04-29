package com.sakura.passage_creator.agreement.model.vo;

import com.sakura.passage_creator.agreement.model.entity.Agreement;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协议返回对象。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = Agreement.class)
public class AgreementVO implements Serializable {

    /**
     * 协议 id。
     */
    private Long id;

    /**
     * 协议类型编码。
     */
    private String agreementType;

    /**
     * 协议标题。
     */
    private String title;

    /**
     * 协议富文本 HTML 内容。
     */
    private String content;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sortOrder;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
