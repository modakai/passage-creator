declare module '@wangeditor/editor-for-vue' {
  import type { DefineComponent } from 'vue'

  /**
   * Toolbar 组件类型补充声明。
   */
  export const Toolbar: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>

  /**
   * Editor 组件类型补充声明。
   */
  export const Editor: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
}
