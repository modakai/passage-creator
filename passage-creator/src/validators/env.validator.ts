import z from 'zod'

export const EnvSchema = z.object({
  VITE_SERVER_API_URL: z.url(),
  VITE_SERVER_API_PREFIX: z.string(),
  VITE_SERVER_API_TIMEOUT: z.coerce.number().default(5000),
  VITE_AUTH_TOKEN_HEADER_NAME: z.string().default('Authorization'),
  VITE_AUTH_TOKEN_HEADER_PREFIX: z.string().default('Bearer '),
  VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME: z.string().default('token'),
  VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED: z.coerce.boolean().default(true),
})

export type env = z.infer<typeof EnvSchema>
