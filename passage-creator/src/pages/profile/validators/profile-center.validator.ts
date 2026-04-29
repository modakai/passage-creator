import { z } from 'zod'

/**
 * 个人资料更新校验器。
 */
export const profileUpdateValidator = z.object({
  userName: z
    .string()
    .trim()
    .min(1, '请输入昵称')
    .max(20, '昵称长度不能超过 20 个字符'),
  userAvatar: z.string().optional(),
})

/**
 * 个人密码更新校验器。
 */
export const profilePasswordValidator = z.object({
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

export type ProfileUpdateValidator = z.infer<typeof profileUpdateValidator>
export type ProfilePasswordValidator = z.infer<typeof profilePasswordValidator>
