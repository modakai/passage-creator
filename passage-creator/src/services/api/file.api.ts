import { useMutation } from '@tanstack/vue-query'

import type { ImageUploadUrl } from '@/services/types/file.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 上传图片到后端文件接口，并返回图片地址。
 */
export function useUploadFileMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<IResponse<ImageUploadUrl>, Error, File>({
    mutationKey: ['file-upload'],
    mutationFn: async (file) => {
      const formData = new FormData()
      formData.append('file', file)

      return await apiFetch<IResponse<ImageUploadUrl>>('/file/upload', {
        method: 'post',
        body: formData,
      })
    },
  })
}
