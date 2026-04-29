import type { ImageUploadItem, ImageUploadStatus } from '@/services/types/file.type'

/**
 * 前端图片上传允许的文件后缀，和后端校验保持一致。
 */
export const IMAGE_UPLOAD_ACCEPTED_EXTENSIONS = ['jpeg', 'jpg', 'svg', 'png', 'webp'] as const

/**
 * 单张图片大小限制，单位为字节。
 */
export const IMAGE_UPLOAD_MAX_SIZE = 1024 * 1024

/**
 * 文件后缀解析正则。
 */
const FILE_EXTENSION_REGEX = /\.([^.]+)$/

/**
 * 根据外部 URL 生成组件内部展示项。
 */
export function createImageUploadItem(input: {
  url: string
  status: ImageUploadStatus
  uid?: string
  name?: string
}): ImageUploadItem {
  return {
    uid: input.uid ?? crypto.randomUUID(),
    url: input.url,
    status: input.status,
    name: input.name ?? resolveImageName(input.url),
  }
}

/**
 * 将成功上传的图片地址回收为组件对外值。
 */
export function collectUploadedUrls(items: ImageUploadItem[]) {
  return items
    .filter(item => item.status === 'success' && item.url)
    .map(item => item.url)
}

/**
 * 合并最新上传成功的地址。
 */
export function mergeUploadedUrl(currentUrls: string[], uploadedUrl: string, maxCount: number) {
  if (maxCount <= 1) {
    return [uploadedUrl]
  }

  return [...currentUrls, uploadedUrl].slice(0, maxCount)
}

/**
 * 校验图片文件是否合法。
 */
export function getImageFileErrorMessage(file: File) {
  const extension = resolveFileExtension(file.name)

  if (!extension || !IMAGE_UPLOAD_ACCEPTED_EXTENSIONS.includes(extension)) {
    return '仅支持 jpeg、jpg、svg、png、webp 格式图片'
  }

  if (file.size > IMAGE_UPLOAD_MAX_SIZE) {
    return '图片大小不能超过 1MB'
  }

  return ''
}

/**
 * 从文件名中解析后缀。
 */
function resolveFileExtension(fileName: string) {
  const [, extension = ''] = fileName.toLowerCase().match(FILE_EXTENSION_REGEX) ?? []
  return extension as typeof IMAGE_UPLOAD_ACCEPTED_EXTENSIONS[number] | ''
}

/**
 * 根据 URL 推导展示名称。
 */
function resolveImageName(url: string) {
  if (!url) {
    return '未命名图片'
  }

  const segments = url.split('/')
  return decodeURIComponent(segments.at(-1) || '已上传图片')
}
