<script setup lang="ts">
import { LoaderCircleIcon, PencilIcon, RefreshCwIcon, SaveIcon, SlidersHorizontalIcon, Trash2Icon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { AiModelPricingForm, AiModelPricingItem, AiModelPricingQuery } from '@/services/types/model-pricing.type'

import { BasicPage } from '@/components/global-layout'
import { useDeleteModelPricingMutation, useGetModelPricingPageQuery, useSaveModelPricingMutation } from '@/services/api/model-pricing.api'

const query = reactive<AiModelPricingQuery>({
  page: 1,
  pageSize: 10,
  provider: '',
  model: '',
  requestType: '',
  enabled: '',
})

const form = reactive<AiModelPricingForm>({
  id: undefined,
  provider: '',
  model: '',
  requestType: 'TEXT',
  promptTokenPricePer1k: 0,
  completionTokenPricePer1k: 0,
  fixedCredits: 0,
  reserveCredits: 1,
  enabled: 1,
})

const { data, isFetching, refetch } = useGetModelPricingPageQuery(query)
const { mutateAsync: savePricing, isPending: isSaving } = useSaveModelPricingMutation()
const { mutateAsync: deletePricing, isPending: isDeleting } = useDeleteModelPricingMutation()
const deletingPricing = ref<AiModelPricingItem | null>(null)

const pricingRows = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const isEditing = computed(() => Boolean(form.id))

function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 重置费率表单，避免编辑上一条记录后新增时复用旧 id。
 */
function resetForm() {
  form.id = undefined
  form.provider = ''
  form.model = ''
  form.requestType = 'TEXT'
  form.promptTokenPricePer1k = 0
  form.completionTokenPricePer1k = 0
  form.fixedCredits = 0
  form.reserveCredits = 1
  form.enabled = 1
}

/**
 * 将表格中的费率配置载入左侧表单，管理员可直接修改并保存。
 */
function editPricing(item: AiModelPricingItem) {
  form.id = item.id
  form.provider = item.provider
  form.model = item.model
  form.requestType = item.requestType
  form.promptTokenPricePer1k = item.promptTokenPricePer1k
  form.completionTokenPricePer1k = item.completionTokenPricePer1k
  form.fixedCredits = item.fixedCredits
  form.reserveCredits = item.reserveCredits
  form.enabled = item.enabled
}

async function submitPricing() {
  if (!form.provider.trim() || !form.model.trim() || !form.requestType.trim()) {
    toast.error('供应商、模型和请求类型不能为空')
    return
  }
  try {
    await savePricing({ ...form })
    toast.success(isEditing.value ? '模型费率已更新' : '模型费率已新增')
    resetForm()
    refetch()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '保存模型费率失败')
  }
}

/**
 * 二次确认后删除费率配置，避免误删影响后续 AI 计费。
 */
async function confirmRemovePricing() {
  if (!deletingPricing.value) {
    return
  }
  try {
    await deletePricing(deletingPricing.value.id)
    toast.success('模型费率已删除')
    deletingPricing.value = null
    refetch()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '删除模型费率失败')
  }
}

function search() {
  query.page = 1
  refetch()
}
</script>

<template>
  <BasicPage title="模型费用配置" description="配置 AI 文本 Token 单价、图片固定成本和调用前预扣积分。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="flex items-center gap-2 text-base">
            <SlidersHorizontalIcon class="size-4" />{{ isEditing ? '编辑费率' : '新增费率' }}
          </UiCardTitle>
          <UiCardDescription>TEXT 按 Token 计费，IMAGE 可使用固定成本。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="space-y-4 pt-5">
          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <div class="space-y-2">
              <UiLabel>供应商</UiLabel>
              <UiInput v-model="form.provider" placeholder="DASHSCOPE / OPENAI" />
            </div>
            <div class="space-y-2">
              <UiLabel>模型</UiLabel>
              <UiInput v-model="form.model" placeholder="qwen3-max / gpt-image-2" />
            </div>
            <div class="space-y-2">
              <UiLabel>请求类型</UiLabel>
              <UiInput v-model="form.requestType" placeholder="TEXT 或 IMAGE" />
            </div>
            <div class="space-y-2">
              <UiLabel>启用状态</UiLabel>
              <UiInput v-model.number="form.enabled" type="number" min="0" max="1" step="1" placeholder="1 启用，0 停用" />
            </div>
          </div>

          <UiSeparator />

          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <div class="space-y-2">
              <UiLabel>输入 Token / 1K</UiLabel>
              <UiInput v-model.number="form.promptTokenPricePer1k" type="number" min="0" step="0.0001" />
            </div>
            <div class="space-y-2">
              <UiLabel>输出 Token / 1K</UiLabel>
              <UiInput v-model.number="form.completionTokenPricePer1k" type="number" min="0" step="0.0001" />
            </div>
            <div class="space-y-2">
              <UiLabel>固定成本</UiLabel>
              <UiInput v-model.number="form.fixedCredits" type="number" min="0" step="0.0001" />
            </div>
            <div class="space-y-2">
              <UiLabel>预扣积分</UiLabel>
              <UiInput v-model.number="form.reserveCredits" type="number" min="0" step="0.0001" />
            </div>
          </div>

          <div class="flex gap-2">
            <UiButton class="flex-1" :disabled="isSaving" @click="submitPricing">
              <SaveIcon class="mr-2 size-4" />保存
            </UiButton>
            <UiButton variant="outline" type="button" @click="resetForm">
              清空
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="text-base">
            费率列表
          </UiCardTitle>
        </UiCardHeader>
        <UiCardContent>
          <div class="grid gap-3 py-5 md:grid-cols-[150px_180px_140px_120px_auto]">
            <UiInput v-model="query.provider" placeholder="供应商" />
            <UiInput v-model="query.model" placeholder="模型" />
            <UiInput v-model="query.requestType" placeholder="TEXT / IMAGE" />
            <UiInput v-model.number="query.enabled" type="number" min="0" max="1" placeholder="启用" />
            <div class="flex gap-2">
              <UiButton @click="search">
                查询
              </UiButton>
              <UiButton variant="outline" @click="query.provider = ''; query.model = ''; query.requestType = ''; query.enabled = ''; search()">
                重置
              </UiButton>
            </div>
          </div>

          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />正在加载模型费率...
          </div>
          <div v-else class="overflow-x-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left">
                <tr class="border-b">
                  <th class="px-4 py-3 font-medium">
                    模型
                  </th>
                  <th class="px-4 py-3 font-medium">
                    类型
                  </th>
                  <th class="px-4 py-3 font-medium">
                    Token 单价
                  </th>
                  <th class="px-4 py-3 font-medium">
                    固定 / 预扣
                  </th>
                  <th class="px-4 py-3 font-medium">
                    状态
                  </th>
                  <th class="px-4 py-3 font-medium">
                    更新时间
                  </th>
                  <th class="px-4 py-3 text-right font-medium">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pricingRows" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.model }}
                    </div>
                    <div class="text-xs text-muted-foreground">
                      {{ item.provider }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge variant="outline">
                      {{ item.requestType }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    <div>输入 {{ formatCredits(item.promptTokenPricePer1k) }}</div>
                    <div>输出 {{ formatCredits(item.completionTokenPricePer1k) }}</div>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    <div>固定 {{ formatCredits(item.fixedCredits) }}</div>
                    <div>预扣 {{ formatCredits(item.reserveCredits) }}</div>
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="item.enabled === 1 ? 'secondary' : 'outline'">
                      {{ item.enabled === 1 ? '启用' : '停用' }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.updateTime) }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex justify-end gap-2">
                      <UiButton variant="outline" size="sm" @click="editPricing(item)">
                        <PencilIcon class="size-4" />
                      </UiButton>
                      <UiButton variant="outline" size="sm" :disabled="isDeleting" @click="deletingPricing = item">
                        <Trash2Icon class="size-4" />
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="pricingRows.length === 0">
                  <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">
                    暂无模型费率配置
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ query.page }} / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="query.page--; refetch()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="query.page++; refetch()">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>

    <UiAlertDialog :open="deletingPricing !== null" @update:open="value => !value ? deletingPricing = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>确认删除模型费率</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            删除后该模型会回退到代码默认费率或找不到数据库配置。请确认不再需要
            {{ deletingPricing?.provider }}/{{ deletingPricing?.model }}。
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="deletingPricing = null">
            取消
          </UiAlertDialogCancel>
          <UiAlertDialogAction :disabled="isDeleting" @click="confirmRemovePricing">
            确认删除
          </UiAlertDialogAction>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>
  </BasicPage>
</template>
