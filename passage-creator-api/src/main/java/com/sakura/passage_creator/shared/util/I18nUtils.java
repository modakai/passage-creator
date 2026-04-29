package com.sakura.passage_creator.shared.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化消息工具。
 *
 * 作者：Sakura
 */
public final class I18nUtils {

    private I18nUtils() {
    }

    /**
     * 读取当前语言环境下的消息。
     *
     * @param messageKey 消息 key
     * @param defaultMessage 默认消息
     * @param args 格式化参数
     * @return 当前语言消息
     */
    public static String getMessage(String messageKey, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            MessageSource messageSource = SpringContextUtils.getBean(MessageSource.class);
            return messageSource.getMessage(messageKey, args, defaultMessage, locale);
        } catch (Exception ignored) {
            return getMessageFromBundle(messageKey, defaultMessage, locale, args);
        }
    }

    /**
     * 解析业务异常里传入的 key 或原始文案。
     *
     * @param messageKeyOrText 消息 key 或原始文案
     * @param args 格式化参数
     * @return 最终消息
     */
    public static String resolveMessage(String messageKeyOrText, Object... args) {
        if (messageKeyOrText == null || messageKeyOrText.isBlank()) {
            return messageKeyOrText;
        }
        if (!messageKeyOrText.contains(".")) {
            return messageKeyOrText;
        }
        return getMessage(messageKeyOrText, messageKeyOrText, args);
    }

    /**
     * 在 Spring 上下文不可用时直接从资源文件读取消息，兼容轻量单元测试。
     *
     * @param messageKey 消息 key
     * @param defaultMessage 默认消息
     * @param locale 当前语言
     * @param args 格式化参数
     * @return 本地化消息
     */
    private static String getMessageFromBundle(String messageKey, String defaultMessage, Locale locale, Object... args) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            String pattern = bundle.getString(messageKey);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException ignored) {
            return defaultMessage;
        }
    }
}
