import type { Response } from "api"

export enum InvitedStatus {
    PENDING = "PENDING",
    ACCEPTED = "ACCEPTED",
    DECLINED = "DECLINED"
}

export type ColumnBoard = {
    kanbanColumnId: number,
    name: string
    position: number
}

export type MemberBoard = {
    kanbanMemberId: number,
    login: string
    avatarUrl: string
    invitedStatus: InvitedStatus,
    invitedAt: string,
    joinedAt: string,
}

export interface ProjetoBoard extends Response {
    kanbanBoardId: number,
    name: string,
    description: string,
    githubRepository: string,
    githubOwnerName: string,
    ownerLogin: string,
    createdAt: string,
    members: MemberBoard[],
    columns: ColumnBoard[]
}