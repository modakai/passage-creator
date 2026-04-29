/**
 * 用户端创建文章任务请求。
 */
export interface AppArticleCreateRequest {
  topic: string
}

/**
 * AI 生成的标题候选。
 */
export interface ArticleTitleOption {
  mainTitle: string
  subTitle: string
}

/**
 * 文章生成 SSE 消息类型。
 */
export type ArticleSseMessageType
  = | 'TITLES_GENERATED'
    | 'OUTLINE_STREAMING'
    | 'OUTLINE_GENERATED'
    | 'CONTENT_STREAMING'
    | 'ALL_COMPLETE'
    | 'ERROR'

/**
 * 文章生成 SSE 消息。
 */
export interface ArticleSseMessage<T = unknown> {
  type: ArticleSseMessageType
  message?: string
  data?: T
}
