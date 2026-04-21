import { useState } from "react";
import { useNotification, useUI } from "context";
import { httpClient, ENDPOINTS } from "api";
import type { ProjetoBoard } from "types";
import { v4 } from "uuid";

type CreatePayload = {
  name: string;
  description: string;
  githubRepository: string;
};

type UpdatePayload = {
  name: string;
  description: string;
};

interface ProjetoFormProps {
  action: "create" | "update";
  board?: ProjetoBoard;
  refetch: () => void;
}

export function ProjetoForm({ action, board, refetch }: ProjetoFormProps) {
  const ui = useUI();
  const { show } = useNotification();

  const [name, setName] = useState(board?.name ?? "");
  const [description, setDescription] = useState(board?.description ?? "");
  const [githubRepository, setGithubRepository] = useState("");
  const [loading, setLoading] = useState(false);

  const isCreate = action === "create";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isCreate) {
        const payload: CreatePayload = { name, description, githubRepository };
        const res = await httpClient.post<ProjetoBoard, CreatePayload>(
          ENDPOINTS.v1.kanban.board.create,
          payload,
        );
        ui.hide("modal", `projeto-form-${action}`);
        window.location.href = `/projetos/${res.kanbanBoardId}/tarefas`;
      } else {
        const payload: UpdatePayload = { name, description };
        await httpClient.put<ProjetoBoard, UpdatePayload>(
          ENDPOINTS.v1.kanban.board.update(board!.kanbanBoardId),
          payload,
        );
        ui.hide("modal", `projeto-form-${action}`);
        refetch();
      }
    } catch (error) {
      show!(
        v4(),
        "Erro projeto",
        "error",
        `Erro ao ${isCreate ? "criar" : "atualizar"} projeto`,
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Nome do Projeto
        </label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: Sistema de Gestão de Tarefas"
          required
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Descrição
        </label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Descreva o objetivo do projeto..."
          rows={3}
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition resize-none"
        />
      </div>

      {isCreate && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Repositório GitHub
          </label>
          <input
            type="text"
            value={githubRepository}
            onChange={(e) => setGithubRepository(e.target.value)}
            placeholder="Ex: planora"
            className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
          />
          <p className="text-xs text-gray-400 mt-1">
            Digite o nome do repositório
          </p>
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={() => ui.hide("modal", `projeto-form-${action}`)}
          className="px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2.5 text-sm font-medium text-white bg-[#0E1F63] rounded-xl hover:bg-[#1a2f7a] transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
        >
          {loading
            ? isCreate
              ? "Criando..."
              : "Salvando..."
            : isCreate
              ? "Criar Projeto"
              : "Salvar Alterações"}
        </button>
      </div>
    </form>
  );
}
