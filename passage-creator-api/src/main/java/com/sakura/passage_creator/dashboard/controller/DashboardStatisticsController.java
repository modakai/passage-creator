package com.sakura.passage_creator.dashboard.controller;

import com.sakura.passage_creator.dashboard.model.vo.DashboardStatisticsVO;
import com.sakura.passage_creator.dashboard.service.DashboardStatisticsService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 Dashboard 统计接口。
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardStatisticsController {

    private final DashboardStatisticsService dashboardStatisticsService;

    public DashboardStatisticsController(DashboardStatisticsService dashboardStatisticsService) {
        this.dashboardStatisticsService = dashboardStatisticsService;
    }

    /**
     * 获取 Dashboard 首页统计。
     *
     * @param request HTTP 请求
     * @return Dashboard 首页统计
     */
    @GetMapping("/statistics")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DashboardStatisticsVO> getStatistics(HttpServletRequest request) {
        return ResultUtils.success(dashboardStatisticsService.getStatistics());
    }
}
