import { useState } from "react";
import { httpClient } from "api";
import { ENDPOINTS } from "../../api/endpoints";
import { useNotification } from "context";

export function InviteMemberForm({
  boardId,
  onClose,
}: {
  boardId: number;
  onClose?: () => void;
}) {
  const notification = useNotification();
  const [login, setLogin] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);

    try {
      await httpClient.post(ENDPOINTS.v1.kanban.board.member.invite(boardId), {
        login,
      });

      notification.show?.(
        "invite-success",
        "Convite enviado",
        "success",
        `Convite enviado para @${login}`,
      );

      onClose?.();
    } catch {
      notification.show?.(
        "invite-error",
        "Erro ao convidar",
        "error",
        "Não foi possível enviar o convite. Verifique o login do usuário.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Login do GitHub
        </label>
        <input
          value={login}
          onChange={(e) => setLogin(e.target.value)}
          placeholder="Ex: planora"
          required
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
        >
          Cancelar
        </button>

        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2.5 text-sm font-medium text-white bg-primary rounded-xl hover:bg-[#1a2f7a] transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
        >
          {loading ? "Enviando..." : "Convidar"}
        </button>
      </div>
    </form>
  );
}
