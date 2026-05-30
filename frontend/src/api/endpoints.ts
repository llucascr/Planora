export const ENDPOINTS = {
  v1: {
    kanban: {
      board: {
        search: (id: number) => `/v1/kanban/board/search/${id}`,
        issue: {
          create: '/v1/kanban/board/issue/create'
        },
        list: '/v1/kanban/board/list',
        create: '/v1/kanban/board/create',
        update: (id: number) => `/v1/kanban/board/update/${id}`,
        delete: (id: number) => `/v1/kanban/board/delete/${id}`,
        member: {
          invite: (boardId: number) => `/v1/kanban/board/${boardId}/member/invite`,
        },
      },
      member: {
        pendingInvites: '/v1/kanban/member/invites/pending',
        updateStatus: (memberId: number) => `/v1/kanban/member/${memberId}/status/update`,
      },
    },
    github: {
      repositories: '/v1/github/repositories',
    },
    dashboard: {
      stats: '/v1/dashboard/stats',
      activity: '/v1/dashboard/activity',
      commits: '/v1/dashboard/commits',
      progress: '/v1/dashboard/progress',
    },
  },
  login: '/login'
} as const;

type DeepValueOf<T> = T extends object ? DeepValueOf<T[keyof T]> : T;
export type Route = DeepValueOf<typeof ENDPOINTS>;
