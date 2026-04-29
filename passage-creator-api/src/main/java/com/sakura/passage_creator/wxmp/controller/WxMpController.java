package com.sakura.passage_creator.wxmp.controller;

import com.sakura.passage_creator.shared.annotation.NoLoginRequired;
import com.sakura.passage_creator.wxmp.constant.WxMpConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts.MenuButtonType;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import me.chanjar.weixin.common.bean.menu.WxMenuButton;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * 微信公众号相关接口。
 *
 * 作者：Sakura
 */
@RestController
@RequestMapping("/")
@Slf4j
@NoLoginRequired
public class WxMpController {

    @Resource
    private WxMpService wxMpService;

    @Resource
    private WxMpMessageRouter router;

    /**
     * 接收公众号消息。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    @PostMapping("/")
    public void receiveMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String signature = request.getParameter("signature");
        String nonce = request.getParameter("nonce");
        String timestamp = request.getParameter("timestamp");
        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            response.getWriter().println("非法请求");
            return;
        }

        String encryptType = StringUtils.defaultIfBlank(request.getParameter("encrypt_type"), "raw");
        if ("raw".equals(encryptType)) {
            return;
        }
        if ("aes".equals(encryptType)) {
            String msgSignature = request.getParameter("msg_signature");
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(
                    request.getInputStream(),
                    wxMpService.getWxMpConfigStorage(),
                    timestamp,
                    nonce,
                    msgSignature
            );
            log.info("message content = {}", inMessage.getContent());
            WxMpXmlOutMessage outMessage = router.route(inMessage);
            if (outMessage == null) {
                response.getWriter().write("");
            } else {
                response.getWriter().write(outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage()));
            }
            return;
        }
        response.getWriter().println("不可识别的加密类型");
    }

    /**
     * 微信服务器校验接口。
     *
     * @param timestamp 时间戳
     * @param nonce 随机串
     * @param signature 签名
     * @param echostr 回显串
     * @return 回显内容
     */
    @GetMapping("/")
    public String check(String timestamp, String nonce, String signature, String echostr) {
        log.info("check");
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        return "";
    }

    /**
     * 设置公众号菜单。
     *
     * @return 固定成功标识
     * @throws WxErrorException 微信异常
     */
    @GetMapping("/setMenu")
    public String setMenu() throws WxErrorException {
        log.info("setMenu");

        WxMenuButton menuButton1 = new WxMenuButton();
        menuButton1.setType(MenuButtonType.VIEW);
        menuButton1.setName("主菜单一");
        WxMenuButton menuButton1SubButton1 = new WxMenuButton();
        menuButton1SubButton1.setType(MenuButtonType.VIEW);
        menuButton1SubButton1.setName("跳转页面");
        menuButton1SubButton1.setUrl("https://yupi.icu");
        menuButton1.setSubButtons(Collections.singletonList(menuButton1SubButton1));

        WxMenuButton menuButton2 = new WxMenuButton();
        menuButton2.setType(MenuButtonType.CLICK);
        menuButton2.setName("点击事件");
        menuButton2.setKey(WxMpConstant.CLICK_MENU_KEY);

        WxMenuButton menuButton3 = new WxMenuButton();
        menuButton3.setType(MenuButtonType.VIEW);
        menuButton3.setName("主菜单三");
        WxMenuButton menuButton3SubButton1 = new WxMenuButton();
        menuButton3SubButton1.setType(MenuButtonType.VIEW);
        menuButton3SubButton1.setName("编程学习");
        menuButton3SubButton1.setUrl("https://yupi.icu");
        menuButton3.setSubButtons(Collections.singletonList(menuButton3SubButton1));

        WxMenu wxMenu = new WxMenu();
        wxMenu.setButtons(Arrays.asList(menuButton1, menuButton2, menuButton3));
        wxMpService.getMenuService().menuCreate(wxMenu);
        return "ok";
    }
}
