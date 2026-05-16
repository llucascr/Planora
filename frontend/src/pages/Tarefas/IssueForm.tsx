import { useState } from "react";
import { httpClient } from "api";
import { useUI } from "context";

interface IssueFormProps {
  action?: "create" | "update";
  issue?: any;

  boardId: number;
  columnId: number;
  repository: string;

  refetch: () => void;
  onClose?: () => void;
}

export function IssueForm({
  action = "create",
  issue,

  boardId,
  columnId,
  repository,

  refetch,
  onClose,
}: IssueFormProps) {
  const ui = useUI();

  const [title, setTitle] = useState(issue?.nome || "");
  const [description, setDescription] = useState(
    issue?.descricao || ""
  );

  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    setLoading(true);

    try {
      if (action === "create") {
        await httpClient.post(
          `/v1/kanban/board/issue/create?boardId=${boardId}&columnId=${columnId}&repository=${repository}`,
          {
            title,
            body: description,
            assignees: [],
            labels: [],
          }
        );
      } else {
        await httpClient.patch(
          `/v1/kanban/board/issue/${issue.id}`,
          {
            title,
            body: description,
          }
        );
      }

      await refetch();

      onClose?.();

      ui.hide(
        "modal",
        action === "create"
          ? "issue-form-create"
          : "issue-form-update"
      );
    } catch (err) {
      console.error(
        action === "create"
          ? "Erro ao criar issue:"
          : "Erro ao editar issue:",
        err
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Título da Issue
        </label>

        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Criar autenticação"
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
          placeholder="Descreva a issue..."
          rows={5}
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none resize-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={() => {
            onClose?.();

            ui.hide(
              "modal",
              action === "create"
                ? "issue-form-create"
                : "issue-form-update"
            );
          }}
          className="px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
        >
          Cancelar
        </button>

        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2.5 text-sm font-medium text-white bg-primary rounded-xl hover:bg-[#1a2f7a] transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
        >
          {loading
            ? action === "create"
              ? "Criando..."
              : "Salvando..."
            : action === "create"
              ? "Criar Issue"
              : "Salvar Alterações"}
        </button>
      </div>
    </form>
  );
}