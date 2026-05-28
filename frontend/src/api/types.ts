export interface Response {
}

export interface DashboardStats {
  totalCommitsLast30Days: number;
  mergedPRsLast30Days: number;
  openPRsCount: number;
  activeBoardsCount: number;
  assignedIssuesLast30Days: number;
}

export interface ActivityDayEntry {
  date: string;
  count: number;
}

export interface CommitHistoryEntry {
  sha: string;
  message: string;
  date: string;
  repositoryName: string;
  url: string;
}

export interface MonthlyProgressEntry {
  date: string;
  opened: number;
  closed: number;
}