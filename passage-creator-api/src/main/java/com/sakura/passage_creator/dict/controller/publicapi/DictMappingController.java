package com.sakura.passage_creator.dict.controller.publicapi;

import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.dict.service.DictMappingService;
import com.sakura.passage_creator.dict.model.dto.DictBatchQueryRequest;
import com.sakura.passage_creator.dict.model.vo.DictItemSimpleVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典映射接口
 *
 * @author sakura
 */
@RestController
@RequestMapping("/dict")
@Validated
public class DictMappingController {

    @Resource
    private DictMappingService dictMappingService;

    /**
     * 获取单个字典映射
     *
     * @param dictCode 字典编码
     * @return 字典映射列表
     */
    @GetMapping("/map")
    public BaseResponse<List<DictItemSimpleVO>> getDictMap(@RequestParam @NotBlank(message = "字典编码不能为空") String dictCode) {
        return ResultUtils.success(dictMappingService.getEnabledItemsByCode(dictCode));
    }

    /**
     * 批量获取字典映射
     *
     * @param request 批量查询请求
     * @return 字典映射结果
     */
    @PostMapping("/map/batch")
    public BaseResponse<Map<String, List<DictItemSimpleVO>>> getDictMapBatch(@Valid @RequestBody DictBatchQueryRequest request) {
        return ResultUtils.success(dictMappingService.getEnabledItemMap(request.getDictCodes()));
    }

    /**
     * 根据编码和值获取标签
     *
     * @param dictCode 字典编码
     * @param value 字典值
     * @return 标签文本
     */
    @GetMapping("/label")
    public BaseResponse<String> getLabelByCodeAndValue(
            @RequestParam @NotBlank(message = "字典编码不能为空") String dictCode,
            @RequestParam @NotBlank(message = "字典值不能为空") String value) {
        return ResultUtils.success(dictMappingService.getLabelByCodeAndValue(dictCode, value));
    }
}



