import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  FolderOpen,
  Plus,
  MagnifyingGlass,
  Columns,
  Users,
  GithubLogo,
  PencilSimple,
  Trash,
} from "@phosphor-icons/react";
import { httpClient, ENDPOINTS } from "api";
import type { ProjetoBoard } from "types";
import { useUI } from "context";
import { ProjetoForm } from "./ProjetoForm";

export const ProjetosPage = () => {
  const ui = useUI();
  const navigate = useNavigate();
  const [kanbanBoards, setKanbanBoards] = useState<ProjetoBoard[]>([]);
  const [search, setSearch] = useState("");
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  function openCreateModal() {
    ui.show({
      id: "projeto-form-create",
      content: <ProjetoForm action="create" refetch={refetch} />,
      type: "modal",
      options: { titulo: "Novo Projeto" },
    });
  }

  function openUpdateModal(board: ProjetoBoard) {
    ui.show({
      id: "projeto-form-update",
      content: <ProjetoForm action="update" board={board} refetch={refetch} />,
      type: "modal",
      options: { titulo: "Editar Projeto" },
    });
  }

  async function handleDelete(id: number) {
    await httpClient.delete(ENDPOINTS.v1.kanban.board.delete(id));
    setConfirmDeleteId(null);
    refetch();
  }

  function refetch() {
    httpClient
      .get<ProjetoBoard[]>(ENDPOINTS.v1.kanban.board.list)
      .then((res) => setKanbanBoards(res));
  }

  useEffect(() => {
    refetch();
  }, []);

  const filtered = kanbanBoards.filter((b) =>
    b.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#0E1F63]">Projetos</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Gerencie e acompanhe todos os seus boards Kanban.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-2 bg-[#0E1F63] text-white text-sm font-medium px-4 py-2.5 rounded-xl hover:bg-[#1a2f7a] transition-colors shadow-sm"
        >
          <Plus size={16} weight="bold" />
          Novo Projeto
        </button>
      </div>

      <div className="relative max-w-sm">
        <MagnifyingGlass
          size={16}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
        />
        <input
          type="text"
          placeholder="Buscar projetos..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-4 py-2.5 text-sm bg-white border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <FolderOpen size={48} weight="duotone" className="mb-3 opacity-40" />
          <p className="text-sm font-medium">Nenhum projeto encontrado</p>
          <p className="text-xs mt-1">
            {kanbanBoards.length === 0
              ? "Crie seu primeiro projeto clicando em Novo Projeto"
              : "Tente ajustar a busca"}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((board) => (
            <div
              key={board.kanbanBoardId}
              onClick={() =>
                navigate(`/projetos/${board.kanbanBoardId}/tarefas`)
              }
              className="bg-white border border-gray-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow cursor-pointer group"
            >
              <div className="flex items-start justify-between mb-2">
                <h3 className="text-sm font-semibold text-gray-800 leading-tight">
                  {board.name}
                </h3>
                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 shrink-0 ml-2">
                  {confirmDeleteId === board.kanbanBoardId ? (
                    <>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(board.kanbanBoardId);
                        }}
                        className="text-xs text-red-500 hover:text-red-700 font-medium px-1.5 py-0.5 rounded transition-colors"
                      >
                        Confirmar
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setConfirmDeleteId(null);
                        }}
                        className="text-xs text-gray-400 hover:text-gray-600 font-medium px-1.5 py-0.5 rounded transition-colors"
                      >
                        Cancelar
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openUpdateModal(board);
                        }}
                        className="text-gray-300 hover:text-gray-500 transition-colors"
                      >
                        <PencilSimple size={15} weight="bold" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setConfirmDeleteId(board.kanbanBoardId);
                        }}
                        className="text-gray-300 hover:text-red-400 transition-colors"
                      >
                        <Trash size={15} weight="bold" />
                      </button>
                    </>
                  )}
                </div>
              </div>

              <p className="text-xs text-gray-400 leading-relaxed line-clamp-2 mb-3">
                {board.description || "Sem descrição"}
              </p>

              {board.githubRepository && (
                <div className="flex items-center gap-1.5 text-xs text-gray-400 mb-3">
                  <GithubLogo size={13} />
                  <span className="truncate">
                    {board.githubOwnerName}/{board.githubRepository}
                  </span>
                </div>
              )}

              <div className="flex items-center justify-between text-xs text-gray-400 pt-3 border-t border-gray-50">
                <span className="flex items-center gap-1">
                  <Columns size={13} />
                  {board.columns.length} colunas
                </span>
                <span className="flex items-center gap-1">
                  <Users size={13} />
                  {board.members.length} membros
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
