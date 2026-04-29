package com.sakura.passage_creator.file.controller;

import cn.hutool.core.io.FileUtil;
import com.sakura.passage_creator.file.service.OssService;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 文件接口
 *
 * @author sakura
 * @from sakura
 */
@RestController
@RequestMapping("/file")
@Slf4j
@ConditionalOnProperty(value = "oss.enable", havingValue = "true")
public class FileController {

    @Resource
    private OssService ossService;

    /**
     * 文件上传
     *
     * @param multipartFile 上传文件
     * @return 文件地址
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        validFile(multipartFile);
        String url = ossService.uploadFile(multipartFile);
        return ResultUtils.success(url);
    }

    /**
     * 校验文件
     *
     * @param multipartFile 上传文件
     */
    private void validFile(MultipartFile multipartFile) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (fileSize > ONE_M) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
        }
        if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
    }
}
