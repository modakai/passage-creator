package com.sakura.passage_creator.wxmp.config;

import com.sakura.passage_creator.wxmp.constant.WxMpConstant;
import com.sakura.passage_creator.wxmp.handler.EventHandler;
import com.sakura.passage_creator.wxmp.handler.MessageHandler;
import com.sakura.passage_creator.wxmp.handler.SubscribeHandler;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.api.WxConsts.EventType;
import me.chanjar.weixin.common.api.WxConsts.XmlMsgType;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号消息路由配置。
 *
 * 作者：Sakura
 */
@Configuration
public class WxMpMsgRouter {

    @Resource
    private WxMpService wxMpService;

    @Resource
    private EventHandler eventHandler;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private SubscribeHandler subscribeHandler;

    /**
     * 注册微信消息路由。
     *
     * @return 消息路由器
     */
    @Bean
    public WxMpMessageRouter getWxMsgRouter() {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        router.rule()
                .async(false)
                .msgType(XmlMsgType.TEXT)
                .handler(messageHandler)
                .end();
        router.rule()
                .async(false)
                .msgType(XmlMsgType.EVENT)
                .event(EventType.SUBSCRIBE)
                .handler(subscribeHandler)
                .end();
        router.rule()
                .async(false)
                .msgType(XmlMsgType.EVENT)
                .event(EventType.CLICK)
                .eventKey(WxMpConstant.CLICK_MENU_KEY)
                .handler(eventHandler)
                .end();
        return router;
    }
}
