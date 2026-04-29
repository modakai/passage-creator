import type { App } from 'vue'

import { createI18n } from 'vue-i18n'

import type { Language } from '.'

import { appLocale, DEFAULT_LOCALE } from '.'
import en from './en.json'
import zh from './zh.json'

/**
 * 全局 i18n 实例，供组件外的工具和表格列定义复用。
 */
export const i18n = createI18n({
  legacy: false,
  locale: appLocale.value,
  fallbackLocale: DEFAULT_LOCALE,
  messages: <Record<Language, Record<string, any>>>{
    zh,
    en,
  },
})

export function setupI18n(app: App) {
  // 让持久化语言与运行中的 i18n 实例保持同步。
  watch(appLocale, (locale) => {
    i18n.global.locale.value = locale
  }, { immediate: true })

  app.use(i18n)
}
