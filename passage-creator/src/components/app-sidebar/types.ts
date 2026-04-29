import type { LucideProps } from '@lucide/vue'
import type { FunctionalComponent } from 'vue'

type NavIcon = FunctionalComponent<LucideProps, Record<any, any>, any, Record<any, any>>

interface BaseNavItem {
  id: string
  title: string
  icon?: NavIcon
}

export type NavItem
  = | BaseNavItem & {
    items: NavItem[]
    url?: never
    isActive?: boolean
  } | BaseNavItem & {
    url: string
    items?: never
  }

export interface NavGroup {
  id: string
  title: string
  items: NavItem[]
}

export interface User {
  name: string
  avatar: string
  email: string
}

export interface Team {
  id: string
  name: string
  logo: NavIcon
  plan: string
}

export interface SidebarData {
  user: User
  navMain: NavGroup[]
}
