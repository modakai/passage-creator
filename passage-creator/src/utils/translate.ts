import { i18n } from '@/plugins/i18n/setup'

/**
 * 给组件外的模块提供统一翻译入口。
 *
 * @param key 国际化 key
 * @returns 翻译后的文本
 */
export function translate(key: string): string {
  const globalComposer = i18n.global as any
  return globalComposer.t(key) as string
}
