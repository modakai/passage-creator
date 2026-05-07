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
 * 文章大纲章节。
 */
export interface ArticleOutlineSection {
  section: number
  title: string
  points: string[]
}

/**
 * 文章大纲结果。
 */
export interface ArticleOutlineResult {
  sections: ArticleOutlineSection[]
}

/**
 * 确认标题并进入大纲生成阶段的请求。
 */
export interface AppArticleConfirmTitleRequest {
  taskId: string
  selectedMainTitle: string
  selectedSubTitle: string
  userDescription?: string
}

/**
 * 确认大纲并进入正文生成阶段的请求。
 */
export interface AppArticleConfirmOutlineRequest {
  taskId: string
  outline: ArticleOutlineResult
}

/**
 * 文章生成阶段。
 */
export type ArticlePhase
  = | 'INPUT'
    | 'PENDING'
    | 'TITLE_GENERATING'
    | 'TITLE_SELECTING'
    | 'OUTLINE_GENERATING'
    | 'OUTLINE_EDITING'
    | 'CONTENT_GENERATING'
    | 'COMPLETED'
    | 'FAILED'

/**
 * 文章任务进度快照。
 */
export interface AppArticleProgress {
  taskId: string
  topic: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  phase?: ArticlePhase
  mainTitle?: string
  subTitle?: string
  titleOptions?: string
  outline?: string
  content?: string
  fullContent?: string
  errorMessage?: string
}

/**
 * 文章生成 SSE 消息类型。
 */
export type ArticleSseMessageType
  = | 'PROGRESS'
    | 'TITLES_GENERATED'
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
