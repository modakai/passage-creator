package com.sakura.passage_creator.shared.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文获取工具
 *
 * @author sakura
 * @from sakura
 */
// 显式指定项目内 Bean 名称，避免与第三方自动配置中的 springContextUtils 冲突。
@Component("sakuraSpringContextUtils")
public class SpringContextUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtils.applicationContext = applicationContext;
    }

    /**
     * 通过名称获取 Bean
     *
     * @param beanName Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * 通过 class 获取 Bean
     *
     * @param beanClass Bean 类型
     * @param <T> Bean 类型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * 通过名称和类型获取 Bean
     *
     * @param beanName Bean 名称
     * @param beanClass Bean 类型
     * @param <T> Bean 类型
     * @return Bean 实例
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return applicationContext.getBean(beanName, beanClass);
    }
}
