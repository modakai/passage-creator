package com.sakura.passage_creator.dict.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.dict.model.dto.DictItemAddRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemQueryRequest;
import com.sakura.passage_creator.dict.model.dto.DictItemUpdateRequest;
import com.sakura.passage_creator.dict.model.entity.DictItem;
import com.sakura.passage_creator.dict.model.vo.DictItemVO;
import com.sakura.passage_creator.dict.service.DictItemService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/dict/item")
@Validated
public class DictItemController {

    @Resource
    private DictItemService dictItemService;

    /**
     * 添加字典数据
     * @return 字典数据id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addDictItem(@Valid @RequestBody DictItemAddRequest request) {
        return ResultUtils.success(dictItemService.addDictItem(request));
    }

    /**
     * 更新字典数据
     * @param request 更新请求
     * @return 更新是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDictItem(@Valid @RequestBody DictItemUpdateRequest request) {
        return ResultUtils.success(dictItemService.updateDictItem(request));
    }

    /**
     * 删除字典数据
     *
     * @param deleteRequest 删除请求
     * @return 删除是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteDictItem(@Valid @RequestBody DeleteRequest deleteRequest) {
        return ResultUtils.success(dictItemService.removeDictItem(deleteRequest.getId()));
    }

    /**
     * 获取字典详情
     * @param id 字典数据id
     * @return 字典数据详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DictItemVO> getDictItemById(@RequestParam @Positive(message = "id 不能为空") long id) {
        DictItem dictItem = dictItemService.getById(id);
        ThrowUtils.throwIf(dictItem == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(dictItemService.getDictItemVO(dictItem));
    }

    /**
     * 分页获取字典明细列表
     *
     * @param queryRequest 查询请求
     * @param httpServletRequest 当前请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<DictItemVO>> listDictItemByPage(@Valid @RequestBody DictItemQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        Page<DictItem> page = dictItemService.page(new Page<>(current, pageSize),
                dictItemService.getQueryWrapper(queryRequest));
        List<DictItemVO> dictItemVOList = dictItemService.getDictItemVO(page.getRecords());
        Page<DictItemVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(dictItemVOList);
        return ResultUtils.success(voPage);
    }
}

