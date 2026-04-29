import { z } from 'zod'

export interface SignUpValidationMessages {
  required: string
  minLength: (min: number) => string
  passwordNotMatch: string
}

export function createSignUpValidator(messages: SignUpValidationMessages) {
  return z
    .object({
      userAccount: z.string().min(1, messages.required).min(4, messages.minLength(4)),
      userPassword: z.string().min(1, messages.required).min(8, messages.minLength(8)),
      checkPassword: z.string().min(1, messages.required).min(8, messages.minLength(8)),
    })
    .refine(data => data.userPassword === data.checkPassword, {
      message: messages.passwordNotMatch,
      path: ['checkPassword'],
    })
}

export type SignUpValidator = z.infer<ReturnType<typeof createSignUpValidator>>
