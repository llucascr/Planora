import { useState } from "react";
import { httpClient } from "api";
import { useUI } from "context";

export function ColumnForm({
  boardId,
  refetch,
  onClose,
}: {
  boardId: number;
  refetch: () => void;
  onClose?: () => void;
}) {
  const ui = useUI();
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);

    await httpClient.post(
      `/v1/kanban/board/${boardId}/column`,
      { name }
    );

    refetch();

    onClose?.();
    ui.hide("modal", "column-form-create");

    setLoading(false);
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Nome da Coluna
        </label>

        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: Em andamento"
          required
          className="w-full px-4 py-2.5 border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={() => {
            onClose?.();
            ui.hide("modal", "column-form-create");
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
          {loading ? "Criando..." : "Criar Coluna"}
        </button>
      </div>
    </form>
  );
}