package com.sakura.passage_creator.infrastructure.aop;

import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.enums.UserRoleEnum;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面。
 *
 * 作者：Sakura
 */
@Aspect
@Component
public class AuthInterceptor {

    /**
     * 对带有权限注解的方法做角色校验。
     *
     * @param joinPoint 切点
     * @param authCheck 权限注解
     * @return 原方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();

        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            // 配置的角色值无法解析时必须默认拒绝，避免鉴权注解失效导致接口被误放行。
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.userRole());
        if (userRoleEnum == null || UserRoleEnum.BAN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
