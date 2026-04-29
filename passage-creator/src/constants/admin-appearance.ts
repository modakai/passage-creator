import type { NavigationMode } from '@/stores/sidebar-config'

import type { ContentLayout, Radius, Theme } from './themes'

import { CONTENT_LAYOUTS, RADIUS, THEME_PRIMARY_COLORS, THEMES } from './themes'

export const ADMIN_THEMES = THEMES
export const ADMIN_RADIUS = RADIUS
export const ADMIN_CONTENT_LAYOUTS = CONTENT_LAYOUTS
export const ADMIN_THEME_PRIMARY_COLORS = THEME_PRIMARY_COLORS

export const COLOR_MODES = ['light', 'dark', 'system'] as const
export type AdminColorMode = typeof COLOR_MODES[number]

export const ADMIN_FONTS = ['system', 'inter', 'manrope', 'geist', 'mono', 'serif'] as const
export type AdminFont = typeof ADMIN_FONTS[number]

export const ADMIN_DENSITIES = ['comfortable', 'standard', 'compact'] as const
export type AdminDensity = typeof ADMIN_DENSITIES[number]

export const ADMIN_COMPONENT_STYLES = ['reka-vega', 'reka-nova', 'reka-maia', 'reka-lyra', 'reka-mira', 'reka-luma'] as const
export type AdminComponentStyle = typeof ADMIN_COMPONENT_STYLES[number]

export const ADMIN_SIDEBAR_STATES = ['remember', 'expanded', 'collapsed'] as const
export type AdminSidebarState = typeof ADMIN_SIDEBAR_STATES[number]

export interface AdminAppearancePreferences {
  colorMode: AdminColorMode
  theme: Theme
  radius: Radius
  font: AdminFont
  density: AdminDensity
  componentStyle: AdminComponentStyle
  contentLayout: ContentLayout
  navigationMode: NavigationMode
  sidebarDefaultState: AdminSidebarState
  reducedMotion: boolean
  stripedTables: boolean
  showBreadcrumbs: boolean
  showPageTitle: boolean
}

// 管理端外观偏好的唯一默认值，所有重置和初始化逻辑都从这里取值。
export const DEFAULT_ADMIN_APPEARANCE_PREFERENCES: AdminAppearancePreferences = {
  colorMode: 'system',
  theme: 'zinc',
  radius: 0.5,
  font: 'system',
  density: 'standard',
  componentStyle: 'reka-vega',
  contentLayout: 'centered',
  navigationMode: 'collapsible',
  sidebarDefaultState: 'remember',
  reducedMotion: false,
  stripedTables: false,
  showBreadcrumbs: true,
  showPageTitle: true,
}

export const ADMIN_FONT_OPTIONS: Array<{ label: string, value: AdminFont, description: string }> = [
  { label: 'System', value: 'system', description: 'Use the operating system font stack.' },
  { label: 'Inter', value: 'inter', description: 'Use a neutral dashboard font.' },
  { label: 'Manrope', value: 'manrope', description: 'Use a slightly rounded interface font.' },
  { label: 'Geist', value: 'geist', description: 'Use a modern Vercel-style interface font.' },
  { label: 'Mono', value: 'mono', description: 'Use a monospace font for technical dashboards.' },
  { label: 'Serif', value: 'serif', description: 'Use a serif font for a more editorial tone.' },
]

export const ADMIN_DENSITY_OPTIONS: Array<{ label: string, value: AdminDensity, description: string }> = [
  { label: 'Comfortable', value: 'comfortable', description: 'Larger spacing for long review sessions.' },
  { label: 'Standard', value: 'standard', description: 'Balanced spacing for daily admin work.' },
  { label: 'Compact', value: 'compact', description: 'Denser spacing for data-heavy workflows.' },
]

export const ADMIN_COMPONENT_STYLE_OPTIONS: Array<{ label: string, value: AdminComponentStyle, description: string }> = [
  { label: 'Reka Vega', value: 'reka-vega', description: 'The default shadcn-vue preset with balanced surfaces.' },
  { label: 'Reka Nova', value: 'reka-nova', description: 'A brighter preset with lifted cards and crisp controls.' },
  { label: 'Reka Maia', value: 'reka-maia', description: 'A compact preset with stronger borders for dense admin screens.' },
  { label: 'Reka Lyra', value: 'reka-lyra', description: 'A calm preset with soft panels and lighter control shadows.' },
  { label: 'Reka Mira', value: 'reka-mira', description: 'A glassier preset with translucent layers and pronounced focus states.' },
  { label: 'Reka Luma', value: 'reka-luma', description: 'A high-contrast preset with direct surfaces and minimal depth.' },
]

export const ADMIN_SIDEBAR_STATE_OPTIONS: Array<{ label: string, value: AdminSidebarState, description: string }> = [
  { label: 'Remember', value: 'remember', description: 'Use the last sidebar state stored by the browser.' },
  { label: 'Expanded', value: 'expanded', description: 'Open the sidebar when entering admin pages.' },
  { label: 'Collapsed', value: 'collapsed', description: 'Collapse the sidebar when entering admin pages.' },
]
