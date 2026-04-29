package com.sakura.passage_creator.infrastructure.generate;

import cn.hutool.core.io.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单代码生成器。
 *
 * 作者：Sakura
 */
public class CodeGenerator {

    /**
     * 运行入口。
     *
     * @param args 启动参数
     * @throws TemplateException 模板异常
     * @throws IOException IO 异常
     */
    public static void main(String[] args) throws TemplateException, IOException {
        // 生成参数示例。
        String packageName = "com.sakura.passage_creator";
        String dataName = "用户评论";
        String dataKey = "userComment";
        String upperDataKey = "UserComment";

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("dataName", dataName);
        dataModel.put("dataKey", dataKey);
        dataModel.put("upperDataKey", upperDataKey);

        String projectPath = System.getProperty("user.dir");
        String inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateController.java.ftl";
        String outputPath = String.format("%s/generator/controller/%sController.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateService.java.ftl";
        outputPath = String.format("%s/generator/service/%sService.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateServiceImpl.java.ftl";
        outputPath = String.format("%s/generator/service/impl/%sServiceImpl.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateAddRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sAddRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateQueryRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sQueryRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateEditRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sEditRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateUpdateRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sUpdateRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);

        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateVO.java.ftl";
        outputPath = String.format("%s/generator/model/vo/%sVO.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
    }

    /**
     * 根据模板生成文件。
     *
     * @param inputPath 模板文件路径
     * @param outputPath 输出文件路径
     * @param model 模板数据
     * @throws IOException IO 异常
     * @throws TemplateException 模板异常
     */
    public static void doGenerate(String inputPath, String outputPath, Object model)
            throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        File templateDir = new File(inputPath).getParentFile();
        configuration.setDirectoryForTemplateLoading(templateDir);
        configuration.setDefaultEncoding("utf-8");

        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);

        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        try (Writer out = new FileWriter(outputPath)) {
            template.process(model, out);
        }
    }
}
