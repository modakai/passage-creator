import { z } from 'zod'

/**
 * 账户偏好设置校验器。
 */
export const accountPreferenceValidator = z.object({
  language: z
    .string()
    .min(1, '请选择默认语言'),
})

/**
 * 账户密码设置校验器。
 */
export const accountPasswordValidator = z.object({
  oldPassword: z.string().min(8, '旧密码长度不能小于 8 位'),
  newPassword: z.string().min(8, '新密码长度不能小于 8 位'),
  checkPassword: z.string().min(8, '确认密码长度不能小于 8 位'),
}).superRefine((value, ctx) => {
  if (value.oldPassword === value.newPassword) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['newPassword'],
      message: '新密码不能与旧密码相同',
    })
  }

  if (value.newPassword !== value.checkPassword) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['checkPassword'],
      message: '两次输入的新密码不一致',
    })
  }
})

export type AccountPreferenceValidator = z.infer<typeof accountPreferenceValidator>
export type AccountPasswordValidator = z.infer<typeof accountPasswordValidator>
