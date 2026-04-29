package com.sakura.passage_creator.shared.util;

import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.fesod.sheet.FesodSheet;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导入导出工具类。
 *
 * @author Sakura
 */
public final class ExcelUtils {

    /**
     * Excel xlsx 响应内容类型。
     */
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /**
     * 默认工作表名称。
     */
    private static final String DEFAULT_SHEET_NAME = "Sheet1";

    /**
     * xlsx 文件后缀。
     */
    private static final String XLSX_SUFFIX = ".xlsx";

    private ExcelUtils() {
    }

    /**
     * 从上传文件读取 Excel 数据。
     *
     * @param file 上传的 Excel 文件
     * @param headClass 行数据类型
     * @param <T> 行数据泛型
     * @return Excel 行数据列表
     */
    public static <T> List<T> read(MultipartFile file, Class<T> headClass) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传 Excel 文件");
        }
        try {
            return read(file.getInputStream(), headClass);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Excel 文件读取失败");
        }
    }

    /**
     * 从输入流读取 Excel 数据。
     *
     * @param inputStream Excel 输入流
     * @param headClass 行数据类型
     * @param <T> 行数据泛型
     * @return Excel 行数据列表
     */
    public static <T> List<T> read(InputStream inputStream, Class<T> headClass) {
        if (inputStream == null || headClass == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Excel 读取参数不能为空");
        }
        try {
            return FesodSheet.read(inputStream)
                    .head(headClass)
                    .sheet()
                    .doReadSync();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Excel 文件读取失败");
        }
    }

    /**
     * 写出 Excel 文件响应。
     *
     * @param fileName 下载文件名
     * @param sheetName 工作表名称
     * @param headClass 行数据类型
     * @param data 行数据列表
     * @param <T> 行数据泛型
     * @return Excel 文件响应
     */
    public static <T> ResponseEntity<byte[]> write(String fileName, String sheetName, Class<T> headClass, List<T> data) {
        byte[] body = writeToBytes(sheetName, headClass, data);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(normalizeFileName(fileName), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /**
     * 写出 Excel 文件字节。
     *
     * @param sheetName 工作表名称
     * @param headClass 行数据类型
     * @param data 行数据列表
     * @param <T> 行数据泛型
     * @return Excel 文件字节
     */
    public static <T> byte[] writeToBytes(String sheetName, Class<T> headClass, List<T> data) {
        if (headClass == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Excel 写入类型不能为空");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            FesodSheet.write(outputStream, headClass)
                    .sheet(normalizeSheetName(sheetName))
                    .doWrite(data == null ? Collections.emptyList() : data);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Excel 文件写入失败");
        }
    }

    /**
     * 规范化下载文件名，避免调用方遗漏 xlsx 后缀。
     */
    private static String normalizeFileName(String fileName) {
        String normalizedFileName = StringUtils.defaultIfBlank(fileName, "export");
        return StringUtils.endsWithIgnoreCase(normalizedFileName, XLSX_SUFFIX)
                ? normalizedFileName
                : normalizedFileName + XLSX_SUFFIX;
    }

    /**
     * 规范化工作表名称，避免空名称导致写入失败。
     */
    private static String normalizeSheetName(String sheetName) {
        return StringUtils.defaultIfBlank(sheetName, DEFAULT_SHEET_NAME);
    }
}
