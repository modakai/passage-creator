import { z } from 'zod'

export interface LoginValidationMessages {
  required: string
}

/**
 * 登录页只校验必填项，账号和密码长度交给后端或具体业务规则处理。
 */
export function createLoginValidator(messages: LoginValidationMessages) {
  return z.object({
    userAccount: z.string().min(1, messages.required),
    userPassword: z.string().min(1, messages.required),
  })
}

export type LoginValidator = z.infer<ReturnType<typeof createLoginValidator>>
