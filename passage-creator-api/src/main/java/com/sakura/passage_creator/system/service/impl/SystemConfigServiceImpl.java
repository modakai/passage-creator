package com.sakura.passage_creator.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.system.model.dto.SystemConfigAddRequest;
import com.sakura.passage_creator.system.model.dto.SystemConfigUpdateRequest;
import com.sakura.passage_creator.system.model.vo.SystemConfigVO;
import com.sakura.passage_creator.system.service.SystemConfigService;
import com.sakura.passage_creator.shared.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现，基于 Redis 保存轻量配置。
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    /**
     * Redis 配置键前缀。
     */
    private static final String CONFIG_KEY_PREFIX = "system:config:";

    @Override
    public SystemConfigVO getByKey(String key) {
        String configValue = RedisUtil.getCacheObject(buildConfigKey(key));
        if (StringUtils.isBlank(configValue)) {
            return null;
        }
        return JSONUtil.toBean(configValue, SystemConfigVO.class);
    }

    @Override
    public boolean addConfig(SystemConfigAddRequest request) {
        String redisKey = buildConfigKey(request.getKey());
        if (Boolean.TRUE.equals(RedisUtil.hasKey(redisKey))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置已存在");
        }
        RedisUtil.setCacheObject(redisKey, JSONUtil.toJsonStr(toVO(request.getKey(), request.getValue(), request.getDescription())));
        return true;
    }

    @Override
    public boolean updateConfig(SystemConfigUpdateRequest request) {
        String redisKey = buildConfigKey(request.getKey());
        if (!Boolean.TRUE.equals(RedisUtil.hasKey(redisKey))) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "配置不存在");
        }
        RedisUtil.setCacheObject(redisKey, JSONUtil.toJsonStr(toVO(request.getKey(), request.getValue(), request.getDescription())));
        return true;
    }

    /**
     * 构造系统配置缓存键。
     *
     * @param key 配置键
     * @return Redis 键
     */
    private String buildConfigKey(String key) {
        return CONFIG_KEY_PREFIX + key;
    }

    /**
     * 构造配置视图对象。
     *
     * @param key 配置键
     * @param value 配置值
     * @param description 配置说明
     * @return 配置视图
     */
    private SystemConfigVO toVO(String key, String value, String description) {
        SystemConfigVO systemConfigVO = new SystemConfigVO();
        systemConfigVO.setKey(key);
        systemConfigVO.setValue(value);
        systemConfigVO.setDescription(description);
        return systemConfigVO;
    }
}
