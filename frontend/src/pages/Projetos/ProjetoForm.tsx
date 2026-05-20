import { useEffect, useMemo, useState } from "react";
import { useNotification, useUI } from "context";
import { httpClient, ENDPOINTS } from "api";
import type { ProjetoBoard } from "types";
import { v4 } from "uuid";
import type { Repositorio } from "@/types/repositorios";
import { Dropdown } from "@processhub-lib/react";
import { CaretDown, GithubLogo, MagicWandIcon } from "@phosphor-icons/react";

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
  const [githubRepository, setGithubRepository] = useState<
    Repositorio | undefined
  >(undefined);
  const [loading, setLoading] = useState(false);
  const [repositories, setRepositories] = useState<Repositorio[]>([]);

  const isCreate = action === "create";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isCreate && githubRepository) {
        const payload: CreatePayload = {
          name,
          description,
          githubRepository: githubRepository.full_name,
        };
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

  function listaRepositorios() {
    httpClient
      .get<Repositorio[]>(ENDPOINTS.v1.github.repositories)
      .then((res) => {
        if (!isCreate && board?.githubRepository) {
          const fullName = board.githubRepository.includes("/")
            ? board.githubRepository
            : `${board.githubOwnerName}/${board.githubRepository}`;
          const match = res.find((r) => r.full_name === fullName);
          if (match) {
            setRepositories(res);
            setGithubRepository(match);
          } else {
            const fallback: Repositorio = {
              id: -1,
              name: board.githubRepository,
              full_name: fullName,
              private: false,
              description: null,
              html_url: "",
            };
            setRepositories([fallback, ...res]);
            setGithubRepository(fallback);
          }
        } else {
          setRepositories(res);
        }
      });
  }

  useEffect(() => {
    listaRepositorios();
  }, []);

  const repoOptions = useMemo(() => {
    return [
      {
        value: "",
        label: "Selecione um repositório",
        icon: <GithubLogo size={16} />,
      },
      ...repositories.map((repo) => ({
        value: String(repo.id),
        label: repo.full_name,
        icon: <GithubLogo size={16} />,
      })),
    ];
  }, [repositories]);

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

      {repositories && (
        <div className="space-y-1.5">
          <label className="block text-sm font-medium text-gray-700">
            Repositório GitHub
          </label>
          <Dropdown
            options={repoOptions}
            value={String(githubRepository?.id || "")}
            onChange={(val) => {
              const repo = repositories.find((r) => String(r.id) === val);
              setGithubRepository(repo);
            }}
            searchable={false}
            className="w-full"
            renderTrigger={(selectedOption, isOpen) => (
              <div className="w-full flex items-center justify-between bg-white border border-gray-200 rounded-xl py-2.5 px-4 text-sm text-gray-700 focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition-all font-normal cursor-pointer shadow-sm hover:border-gray-300">
                <div className="flex items-center gap-2 truncate">
                  {selectedOption?.icon || <GithubLogo size={16} />}
                  <span className="truncate">
                    {selectedOption?.label || "Selecione um repositório"}
                  </span>
                </div>
                <CaretDown
                  size={12}
                  className={`transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}
                />
              </div>
            )}
            menuClassName="!w-full h-64 overflow-y-auto !rounded-xl !border-gray-100 !shadow-2xl !bg-white"
          />
        </div>
      )}

      <div>
        <div className="flex items-center justify-between mb-1.5">
          <label className="block text-sm font-medium text-gray-700">
            Descrição
          </label>
          {isCreate && githubRepository?.description && !description && (
            <button
              type="button"
              onClick={() => setDescription(githubRepository.description || "")}
              className="flex items-center gap-1.5 text-xs font-medium text-primary hover:text-primary/80 transition-colors"
              title="Usar descrição do GitHub"
            >
              <MagicWandIcon size={14} />
              <span>Sincronizar com GitHub</span>
            </button>
          )}
        </div>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Descreva o objetivo do projeto..."
          rows={3}
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition resize-none"
        />
      </div>

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
          className="px-4 py-2.5 text-sm font-medium text-white bg-primary rounded-xl hover:bg-[#1a2f7a] transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
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
