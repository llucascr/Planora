import { useState } from "react";
import { httpClient } from "api";
import { useUI } from "context";
import { CaretDown, Check } from "@phosphor-icons/react";
import type { MemberBoard } from "types";

interface IssueFormProps {
  action?: "create" | "update";
  issue?: any;

  boardId: number;
  columnId: number;
  repository: string;

  members: MemberBoard[];

  refetch: () => void;
  onClose?: () => void;
}

export function IssueForm({
  action = "create",
  issue,
  boardId,
  columnId,
  repository,
  members,
  refetch,
  onClose,
}: IssueFormProps) {
  const ui = useUI();

  const [title, setTitle] = useState(issue?.nome || "");
  const [description, setDescription] = useState(
    issue?.descricao || ""
  );
  const [assignees, setAssignees] = useState<string[]>(
    issue?.assignees?.map((a: any) => a.login) || []
  );
  const [openMembers, setOpenMembers] = useState(false);
  const [loading, setLoading] = useState(false);

  function toggleAssignee(login: string) {
    setAssignees((prev) =>
      prev.includes(login)
        ? prev.filter((a) => a !== login)
        : [...prev, login]
    );
  }

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
            assignees,
            labels: [],
          }
        );
      } else {
        await httpClient.patch(
          `/v1/kanban/board/issue/${issue.id}`,
          {
            title,
            body: description,
            assignees,
            labels: [],
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

      <div className="relative">
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Responsáveis
        </label>

        <button
          type="button"
          onClick={() => setOpenMembers((prev) => !prev)}
          className="w-full flex items-center justify-between px-4 py-2.5 border border-gray-200 rounded-xl bg-white text-left hover:border-gray-300 transition"
        >
          <span className="text-sm text-gray-700 truncate">
            {assignees.length === 0
              ? "Selecionar responsáveis"
              : assignees.join(", ")}
          </span>

          <CaretDown
            size={16}
            className={`transition-transform ${openMembers ? "rotate-180" : ""
              }`}
          />
        </button>

        {openMembers && (
          <div className="absolute z-20 mt-2 w-full bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden">
            <div className="max-h-52 overflow-y-auto">
              {members.length === 0 ? (
                <p className="px-4 py-3 text-sm text-gray-400">
                  Nenhum membro encontrado
                </p>
              ) : (
                members.map((member) => {
                  const selected = assignees.includes(member.login);

                  return (
                    <button
                      key={member.login}
                      type="button"
                      onClick={() => toggleAssignee(member.login)}
                      className="w-full flex items-center justify-between px-4 py-2.5 hover:bg-gray-50 transition text-left"
                    >
                      <span className="text-sm text-gray-700">
                        {member.login}
                      </span>

                      {selected && (
                        <Check
                          size={16}
                          weight="bold"
                          className="text-primary"
                        />
                      )}
                    </button>
                  );
                })
              )}
            </div>
          </div>
        )}
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