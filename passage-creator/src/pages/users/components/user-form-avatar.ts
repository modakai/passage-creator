/**
 * 将用户头像字符串转换为图片上传组件需要的数组模型。
 */
export function buildAvatarUploadModel(userAvatar?: string | null) {
  const avatarUrl = userAvatar?.trim()

  return avatarUrl ? [avatarUrl] : []
}

/**
 * 图片上传组件返回数组，这里只取第一张作为用户头像。
 */
export function resolveAvatarFromUploadModel(urls: string[]) {
  return urls[0]?.trim() ?? ''
}
