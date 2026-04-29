import { AwardIcon, BadgeDollarSignIcon, HandshakeIcon, ShieldIcon } from '@lucide/vue'
import { h } from 'vue'

import type { FacetedFilterOption } from '@/components/data-table'

import { translate } from '@/utils/translate'

/**
 * 用户状态筛选项。
 */
export function getCallTypes(): (FacetedFilterOption & { style: string })[] {
  return [
    {
      label: translate('pages.users.status.active'),
      value: 'active',
      style: 'bg-teal-100/30 text-teal-900 dark:text-teal-200 border-teal-200',
    },
    {
      label: translate('pages.users.status.inactive'),
      value: 'inactive',
      style: 'bg-neutral-300/40 border-neutral-300',
    },
    {
      label: translate('pages.users.status.invited'),
      value: 'invited',
      style: 'bg-sky-200/40 text-sky-900 dark:text-sky-100 border-sky-300',
    },
    {
      label: translate('pages.users.status.suspended'),
      value: 'suspended',
      style: 'bg-destructive/10 dark:bg-destructive/50 text-destructive dark:text-primary border-destructive/10',
    },
  ]
}

/**
 * 用户角色筛选项。
 */
export function getUserTypes(): FacetedFilterOption[] {
  return [
    {
      label: translate('pages.users.roles.superadmin'),
      value: 'superadmin',
      icon: h(BadgeDollarSignIcon),
    },
    {
      label: translate('pages.users.roles.admin'),
      value: 'admin',
      icon: h(HandshakeIcon),
    },
    {
      label: translate('pages.users.roles.manager'),
      value: 'manager',
      icon: h(AwardIcon),
    },
    {
      label: translate('pages.users.roles.cashier'),
      value: 'cashier',
      icon: h(ShieldIcon),
    },
  ] as const
}
