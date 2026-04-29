import { z } from 'zod'

/**
 * 后台个人资料校验器。
 */
export const settingsProfileValidator = z.object({
  userName: z
    .string()
    .trim()
    .min(1, '请输入昵称')
    .max(20, '昵称长度不能超过 20 个字符'),
  userAvatar: z.string().optional(),
})

export type SettingsProfileValidator = z.infer<typeof settingsProfileValidator>
