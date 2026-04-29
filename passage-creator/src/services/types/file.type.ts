/**
 * 图片上传成功后返回的图片地址。
 */
export type ImageUploadUrl = string

/**
 * 图片上传项状态。
 */
export type ImageUploadStatus = 'uploading' | 'success' | 'error'

/**
 * 图片上传组件内部使用的展示项。
 */
export interface ImageUploadItem {
  uid: string
  url: string
  status: ImageUploadStatus
  name: string
}
