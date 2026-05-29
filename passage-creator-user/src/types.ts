export interface ApiResponse<T> {
  code?: number
  data?: T
  message?: string
}

export interface PageResponse<T> {
  records: T[]
  totalRow: number
}

export interface ArticleTitleOption {
  mainTitle: string
  subTitle: string
}

export interface ArticleOutlineSection {
  section: number
  title: string
  points: string[]
}

export interface ArticleOutlineResult {
  sections: ArticleOutlineSection[]
}

export interface ArticleImageResult {
  position?: number
  url?: string
  method?: string
  keywords?: string
  sectionTitle?: string
  description?: string
}

export type ArticlePhase =
  | 'INPUT'
  | 'PENDING'
  | 'TITLE_GENERATING'
  | 'TITLE_SELECTING'
  | 'OUTLINE_GENERATING'
  | 'OUTLINE_EDITING'
  | 'CONTENT_GENERATING'
  | 'IMAGE_ANALYZING'
  | 'IMAGE_GENERATING'
  | 'CONTENT_MERGING'
  | 'COMPLETED'
  | 'EXPIRED'
  | 'FAILED'

export interface AppArticleItem {
  id?: number
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
  coverImage?: string
  images?: string
  errorMessage?: string
  createTime?: string
  completedTime?: string
  updateTime?: string
}

export interface AppRednoteItem {
  id?: number
  taskId: string
  content?: string
  subject?: string
  context?: string
  status?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  phase?: RednotePhase
  bodyContent?: string
  coverTitle?: string
  coverImage?: string
  imageCount?: number
  keywords?: string
  searchResults?: string
  tags?: string
  imagePrompts?: string
  images?: string
  errorMessage?: string
  createTime?: string
  updateTime?: string
  completedTime?: string
}

export type RednotePhase =
  | 'PENDING'
  | 'SEARCH_AGENT'
  | 'COPY_GENERATING'
  | 'IMAGE_PROMPT_GENERATING'
  | 'IMAGE_GENERATING'
  | 'COMPLETED'
  | 'FAILED'

export interface SseMessage<T = unknown> {
  type: string
  message?: string
  taskId?: string
  data?: T
  payload?: Record<string, unknown>
  nodeType?: string
}
