import { defineComponent, h, type PropType } from 'vue'

type HugeIconNode = readonly [string, Readonly<Record<string, string | number>>]
export type HugeIconData = readonly HugeIconNode[]

/**
 * 极薄的 Hugeicons 渲染器，用 core-free-icons 的 SVG 数据生成 Vue 节点。
 */
export default defineComponent({
  name: 'HugeIcon',
  inheritAttrs: false,
  props: {
    icon: {
      type: Array as PropType<HugeIconData>,
      required: true,
    },
    size: {
      type: [Number, String],
      default: 24,
    },
    strokeWidth: {
      type: [Number, String],
      default: 1.5,
    },
  },
  setup(props, { attrs }) {
    return () => h(
      'svg',
      {
        ...attrs,
        width: props.size,
        height: props.size,
        viewBox: '0 0 24 24',
        fill: 'none',
        xmlns: 'http://www.w3.org/2000/svg',
      },
      props.icon.map(([tag, nodeAttrs], index) => h(tag, {
        ...nodeAttrs,
        key: index,
        'stroke-width': props.strokeWidth,
      })),
    )
  },
})
