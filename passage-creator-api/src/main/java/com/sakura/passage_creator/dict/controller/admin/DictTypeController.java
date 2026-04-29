package com.sakura.passage_creator.dict.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import com.sakura.passage_creator.dict.model.entity.DictType;
import com.sakura.passage_creator.dict.service.DictTypeService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.dict.model.dto.DictTypeAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictTypeUpdateRequest;
import com.sakura.passage_creator.dict.model.vo.DictTypeVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台字典类型接口
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/dict/type")
@Validated
public class DictTypeController {

    @Resource
    private DictTypeService dictTypeService;

    /**
     * 新增字典类型
     *
     * @param request 请求参数
     * @param httpServletRequest 当前请求
     * @return 新增记录 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addDictType(@Valid @RequestBody DictTypeAddRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(dictTypeService.addDictType(request));
    }

    /**
     * 更新字典类型
     *
     * @param request 请求参数
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDictType(@Valid @RequestBody DictTypeUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(dictTypeService.updateDictType(request));
    }

    /**
     * 删除字典类型
     *
     * @param deleteRequest 删除请求
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteDictType(@Valid @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(dictTypeService.removeDictType(deleteRequest.getId()));
    }

    /**
     * 根据 id 获取字典类型
     *
     * @param id 类型 id
     * @param httpServletRequest 当前请求
     * @return 字典类型详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DictTypeVO> getDictTypeById(@RequestParam @Positive(message = "字典类型 id 必须大于 0") long id,
            HttpServletRequest httpServletRequest) {
        DictType dictType = dictTypeService.getById(id);
        ThrowUtils.throwIf(dictType == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(dictTypeService.getDictTypeVO(dictType));
    }

    /**
     * 分页获取字典类型列表
     *
     * @param queryRequest 查询请求
     * @param httpServletRequest 当前请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<DictTypeVO>> listDictTypeByPage(@Valid @RequestBody DictTypeQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        Page<DictType> page = dictTypeService.page(new Page<>(current, pageSize),
                dictTypeService.getQueryWrapper(queryRequest));
        List<DictTypeVO> dictTypeVOList = dictTypeService.getDictTypeVO(page.getRecords());
        Page<DictTypeVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(dictTypeVOList);
        return ResultUtils.success(voPage);
    }
}



