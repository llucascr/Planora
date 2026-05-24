import { httpClient } from "api";
import { Board, type BoardColumn } from "components";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import type { ProjetoBoard } from "types";
import { useUI } from "context";
import { ColumnForm } from "./ColumnForm";
import { InviteMemberForm } from "./InviteMemberForm";

export const TarefasPage = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const boardId = Number(projectId);

  const [columns, setColumns] = useState<BoardColumn[]>([]);
  const [board, setBoard] = useState<ProjetoBoard | null>(null);
  const ui = useUI();

  function refetch() {
    httpClient
      .get<ProjetoBoard>(`/v1/kanban/board/${boardId}`)
      .then((res) => {
        setBoard(res);
        const mappedColumns: BoardColumn[] = res.columns.map((col) => ({
          id: Number(col.kanbanColumnId),
          name: col.name,
          order: col.position,
          idBoard: res.kanbanBoardId,
          cards: [],
        }));

        setColumns(mappedColumns);
      })
      .catch((err) => {
        console.error("Erro ao buscar board:", err);
      });
  }

  function openCreateColumnModal() {
    ui.show({
      id: "column-form-create",
      type: "modal",
      options: { titulo: "Nova Coluna" },
      content: (
        <ColumnForm
          boardId={boardId}
          refetch={refetch}
          onClose={() => ui.hide("modal", "column-form-create")}
        />
      ),
    });
  }

  function openInviteMemberModal() {
    ui.show({
      id: "invite-member",
      type: "modal",
      options: { titulo: "Convidar Membro" },
      content: (
        <InviteMemberForm
          boardId={boardId}
          onClose={() => ui.hide("modal", "invite-member")}
        />
      ),
    });
  }

  useEffect(() => {
    if (!boardId || isNaN(boardId)) return;
    refetch();
  }, [boardId]);

  async function handleColumnMove(
    fromIndex: number,
    toIndex: number,
    columnId: string,
  ) {
    const reordered = [...columns];

    const [moved] = reordered.splice(fromIndex, 1);
    reordered.splice(toIndex, 0, moved);

    await httpClient.put(`/v1/kanban/board/${boardId}/column/${columnId}`, {
      position: toIndex,
    });

    setColumns(reordered);
  }

  return (
    <div className="flex flex-col h-full space-y-5">
      <Board
        key={columns.length}
        columns={columns}
        refetch={refetch}
        onColumnMove={handleColumnMove}
        onCreateColumn={openCreateColumnModal}
        onInviteMember={openInviteMemberModal}
        boardId={boardId}
        members={board?.members}
        repository={board?.githubRepository}
        githubOwnerName={board?.githubOwnerName}
      />
    </div>
  );
};
