package com.sakura.passage_creator.observability.service;

import com.sakura.passage_creator.observability.model.vo.SystemStatusVO;

/**
 * 系统状态聚合服务。
 *
 * @author Sakura
 */
public interface SystemStatusService {

    /**
     * 获取当前系统状态快照。
     *
     * @return 系统状态视图
     */
    SystemStatusVO getSystemStatus();
}
