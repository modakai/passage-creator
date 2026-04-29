import type { Router } from 'vue-router'

import { setupDocumentTitle } from '@/utils/document-title'

import { setupAuthGuard } from './auth-guard'
import { setupCommonGuard } from './common-guard'

export function setupRouterGuard(router: Router) {
  setupCommonGuard(router)
  setupAuthGuard(router)
  setupDocumentTitle(router)
}
