<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'

import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'

import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

/**
 * 富文本编辑器属性。
 */
const props = withDefaults(defineProps<{
  modelValue: string
  readonly?: boolean
  height?: number
}>(), {
  readonly: false,
  height: 360,
})

/**
 * 富文本编辑器事件。
 */
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

/**
 * 保存编辑器实例，便于组件销毁时释放资源。
 */
const editorRef = shallowRef<IDomEditor>()

/**
 * 工具栏配置，关闭当前阶段不支持的图片/视频上传入口。
 */
const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['uploadImage', 'uploadVideo', 'insertVideo'],
}

/**
 * 编辑器配置，使用可信后台 HTML 内容直接编辑。
 */
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入协议内容...',
  readOnly: props.readonly,
  autoFocus: false,
}

/**
 * 同步外部值到内部编辑器状态。
 */
const innerHtml = ref(props.modelValue)

watch(() => props.modelValue, (value) => {
  if (value !== innerHtml.value) {
    innerHtml.value = value
  }
})

watch(() => props.readonly, (value) => {
  const editor = editorRef.value
  if (!editor) {
    return
  }
  if (value) {
    editor.disable()
    return
  }
  editor.enable()
})

/**
 * 记录编辑器实例。
 */
function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
  if (props.readonly) {
    editor.disable()
  }
}

/**
 * 将编辑器内容同步回表单。
 */
function handleChange(editor: IDomEditor) {
  const html = editor.getHtml()
  innerHtml.value = html
  emit('update:modelValue', html)
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="overflow-hidden rounded-lg border bg-background">
    <Toolbar
      v-if="!readonly"
      :editor="editorRef"
      :default-config="toolbarConfig"
      mode="default"
      class="border-b"
    />
    <Editor
      v-model="innerHtml"
      :default-config="editorConfig"
      mode="default"
      :style="{ minHeight: `${height}px` }"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>
