package com.sakura.passage_creator.infrastructure.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码哈希服务，集中封装 BCrypt 哈希和校验逻辑。
 */
@Component
public class PasswordHashService {

    /**
     * BCrypt 哈希格式校验，提前挡住损坏数据，避免底层 encoder 记录噪声日志。
     */
    private static final Pattern BCRYPT_HASH_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    /**
     * BCrypt 默认强度为 10，能兼顾当前登录性能和离线撞库成本。
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 对明文密码生成 BCrypt 哈希。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 密码哈希
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文密码和已保存哈希是否匹配。
     *
     * @param rawPassword 明文密码
     * @param storedHash 已保存密码哈希
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (StringUtils.isAnyBlank(rawPassword, storedHash) || !BCRYPT_HASH_PATTERN.matcher(storedHash).matches()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
