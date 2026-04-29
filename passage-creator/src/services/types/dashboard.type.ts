export interface DashboardSummary {
  userTotalCount: number
  todayNewUserCount: number
  notificationCount: number
  operationLogCount: number
}

export interface DashboardLoginTrend {
  label: string
  startTime: string
  endTime: string
  loginCount: number
}

export interface DashboardRecentOperation {
  id: number
  operator: string
  action: string
  module: string
  operationType: string
  result: string
  ipAddress: string
  operationTime: string
}

export interface DashboardStatistics {
  summary: DashboardSummary
  loginTrend: DashboardLoginTrend[]
  recentOperations: DashboardRecentOperation[]
  sampleTime: string
}
