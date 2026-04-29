import { computed, ref } from 'vue'

const globalLoadingCount = ref(0)
const debounceTimers = new Map<string, ReturnType<typeof setTimeout>>()

/**
 * 只要存在一个正在跟踪的请求，就展示全局 Loading。
 */
export const globalLoadingVisible = computed(() => globalLoadingCount.value > 0)

/**
 * 开始一次全局请求 Loading，并返回只会生效一次的结束函数。
 */
export function startGlobalLoading() {
  let finished = false
  globalLoadingCount.value += 1

  return () => {
    if (finished) {
      return
    }
    finished = true
    finishGlobalLoading()
  }
}

/**
 * 结束一次全局请求 Loading，防止异常路径导致计数跌成负数。
 */
export function finishGlobalLoading() {
  globalLoadingCount.value = Math.max(0, globalLoadingCount.value - 1)
}

/**
 * 根据请求关键信息生成稳定指纹，用于判断短时间内的重复请求。
 */
export function getRequestDebounceKey(request: string, options: Record<string, any> = {}) {
  const method = String(options.method ?? 'GET').toUpperCase()
  const query = stableStringify(options.query ?? null)
  const body = isFormDataLike(options.body) ? '[form-data]' : stableStringify(options.body ?? null)

  return `${method}:${request}:${query}:${body}`
}

/**
 * 注册请求指纹；返回 false 表示该请求仍在防抖窗口内，应被拦截。
 */
export function registerRequestDebounce(key: string, delay = 500) {
  if (debounceTimers.has(key)) {
    return false
  }

  const timer = setTimeout(() => {
    debounceTimers.delete(key)
  }, delay)
  debounceTimers.set(key, timer)
  return true
}

/**
 * 清空防抖状态，主要用于测试和页面级重置。
 */
export function clearRequestDebounce() {
  debounceTimers.forEach(timer => clearTimeout(timer))
  debounceTimers.clear()
}

/**
 * 按对象键名排序后序列化，避免同一查询参数因字段顺序不同绕过防抖。
 */
function stableStringify(value: unknown): string {
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value)
  }

  if (Array.isArray(value)) {
    return `[${value.map(item => stableStringify(item)).join(',')}]`
  }

  const entries = Object.entries(value as Record<string, unknown>)
    .filter(([, entryValue]) => entryValue !== undefined)
    .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))

  return `{${entries.map(([key, entryValue]) => `${JSON.stringify(key)}:${stableStringify(entryValue)}`).join(',')}}`
}

/**
 * FormData 可能包含 File/Blob，不做内容展开，避免读取文件对象带来副作用。
 */
function isFormDataLike(value: unknown) {
  return typeof FormData !== 'undefined' && value instanceof FormData
}
