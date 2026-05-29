/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_SERVER_API_URL?: string
  readonly VITE_SERVER_API_PREFIX?: string
  readonly VITE_AUTH_TOKEN_HEADER_NAME?: string
  readonly VITE_AUTH_TOKEN_HEADER_PREFIX?: string
  readonly VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME?: string
  readonly VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
