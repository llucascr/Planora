import { httpClient } from "api";
import { Board, type BoardColumn } from "components";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import type { ProjetoBoard } from "types";
import { useUI } from "context";
import { ColumnForm } from "./ColumnForm";

// const projectsData: Record<string, { name: string; color: string }> = {
//   "1": { name: "Redesign do App", color: "#0E1F63" },
//   "2": { name: "API de Pagamentos", color: "#3d5aad" },
//   "3": { name: "Dashboard Analytics", color: "#16a34a" },
//   "4": { name: "Módulo de Relatórios", color: "#dc2626" },
//   "5": { name: "App Mobile", color: "#0E1F63" },
//   "6": { name: "Portal do Cliente", color: "#3d5aad" },
// };

// const initialColumns: BoardColumn[] = [
//   {
//     id: 1,
//     idBoard: 1,
//     nome: "Teste 01",
//     ordem: 1,
//     cards: [
//       {
//         id: 1,
//         codigo: 123,
//         createdAt: "2026-03-23T11:57:28.000Z",
//         descricao: "teste",
//         nome: "teste 01",
//         planoAcao: null,
//         lead: null,
//       },
//     ],
//   },

//   {
//     id: 2,
//     idBoard: 1,
//     nome: "Teste 02",
//     ordem: 2,
//     cards: [
//       {
//         id: 2,
//         codigo: 123,
//         createdAt: "2026-03-20T11:57:28.000Z",
//         descricao: "teste",
//         nome: "teste 02",
//         planoAcao: null,
//         lead: null,
//       },
//     ],
//   },
// ];

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

  useEffect(() => {
    if (!boardId || isNaN(boardId)) return;
    refetch();
  }, [boardId]);

  async function handleColumnMove(
    fromIndex: number,
    toIndex: number,
    columnId: string
  ) {
    const reordered = [...columns];

    const [moved] = reordered.splice(fromIndex, 1);
    reordered.splice(toIndex, 0, moved);

    await httpClient.put(
      `/v1/kanban/board/${boardId}/column/${columnId}`,
      {
        position: toIndex,
      }
    );

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
        boardId={boardId}
        members={board?.members}
        repository={board?.githubRepository}
      />
    </div>
  );
};
