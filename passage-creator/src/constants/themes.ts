import { MoveHorizontalIcon, UnfoldHorizontalIcon } from '@lucide/vue'

// 管理端可选主题色，顺序按 shadcn-vue Colors 页面中的 Tailwind 色族分组展示。
export const THEMES = ['slate', 'gray', 'zinc', 'neutral', 'stone', 'red', 'rose', 'pink', 'fuchsia', 'purple', 'violet', 'indigo', 'blue', 'sky', 'cyan', 'teal', 'emerald', 'green', 'lime', 'yellow', 'amber', 'orange'] as const
export type Theme = typeof THEMES[number]

// 主题色块预览使用主色，不直接参与 CSS 变量应用。
export const THEME_PRIMARY_COLORS: { theme: Theme, primaryColor: string }[] = [
  { theme: 'slate', primaryColor: 'oklch(0.208 0.042 265.755)' },
  { theme: 'gray', primaryColor: 'oklch(0.21 0.034 264.665)' },
  { theme: 'zinc', primaryColor: 'oklch(44.2% 0.017 285.786)' },
  { theme: 'neutral', primaryColor: 'oklch(0.205 0 0)' },
  { theme: 'stone', primaryColor: 'oklch(0.216 0.006 56.043)' },
  { theme: 'red', primaryColor: 'oklch(57.7% 0.245 27.325)' },
  { theme: 'rose', primaryColor: 'oklch(0.645 0.246 16.439)' },
  { theme: 'pink', primaryColor: 'oklch(59.2% 0.249 0.584)' },
  { theme: 'fuchsia', primaryColor: 'oklch(59.1% 0.293 322.896)' },
  { theme: 'purple', primaryColor: 'oklch(55.8% 0.288 302.321)' },
  { theme: 'violet', primaryColor: 'oklch(0.606 0.25 292.717)' },
  { theme: 'indigo', primaryColor: 'oklch(51.1% 0.262 276.966)' },
  { theme: 'blue', primaryColor: 'oklch(48.8% 0.243 264.376)' },
  { theme: 'sky', primaryColor: 'oklch(58.8% 0.158 241.966)' },
  { theme: 'cyan', primaryColor: 'oklch(60.9% 0.126 221.723)' },
  { theme: 'teal', primaryColor: 'oklch(60% 0.118 184.704)' },
  { theme: 'emerald', primaryColor: 'oklch(59.6% 0.145 163.225)' },
  { theme: 'green', primaryColor: 'oklch(0.723 0.219 149.579)' },
  { theme: 'lime', primaryColor: 'oklch(64.8% 0.2 131.684)' },
  { theme: 'yellow', primaryColor: 'oklch(68.1% 0.162 75.834)' },
  { theme: 'amber', primaryColor: 'oklch(66.6% 0.179 58.318)' },
  { theme: 'orange', primaryColor: 'oklch(0.705 0.213 47.604)' },
] as const

export type Radius = typeof RADIUS[number]
export const RADIUS = [0, 0.25, 0.5, 0.75, 1] as const

export type ContentLayout = 'full' | 'centered'
export const CONTENT_LAYOUTS = [
  { label: 'Full', value: 'full', icon: UnfoldHorizontalIcon },
  { label: 'Centered', value: 'centered', icon: MoveHorizontalIcon },
] as const
