// ⚠️ Arquivo gerado automaticamente — NÃO EDITE

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
      }
    },
    github: {
      repositories: '/v1/github/repositories',
    }
  },
  login: '/login'
} as const;

type DeepValueOf<T> = T extends object ? DeepValueOf<T[keyof T]> : T;
export type Route = DeepValueOf<typeof ENDPOINTS>;
