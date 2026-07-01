import { useEffect, useRef, useState } from "react";
import { Bell, CheckCircle, XCircle, FolderOpen } from "@phosphor-icons/react";
import { httpClient } from "api";
import { ENDPOINTS } from "../../api/endpoints";
import type { PendingInvite } from "types";
import { useNotification } from "context";

export function PendingInvitesPanel() {
  const [open, setOpen] = useState(false);
  const [invites, setInvites] = useState<PendingInvite[]>([]);
  const [loading, setLoading] = useState(false);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const notification = useNotification();

  async function fetchInvites() {
    setLoading(true);
    try {
      const data = await httpClient.get<PendingInvite[]>(ENDPOINTS.v1.kanban.member.pendingInvites);
      setInvites(data);
    } catch {
      // silently fail — user may not be logged in yet
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchInvites();
  }, []);

  useEffect(() => {
    if (!open) return;
    fetchInvites();
  }, [open]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function respond(memberId: number, status: "ACCEPTED" | "DECLINED") {
    setProcessingId(memberId);
    try {
      await httpClient.patch(
        `${ENDPOINTS.v1.kanban.member.updateStatus(memberId)}?status=${status}`,
        null
      );
      setInvites((prev) => prev.filter((i) => i.kanbanMemberId !== memberId));
      if (status === "ACCEPTED") {
        window.dispatchEvent(new CustomEvent("planora:projects:refetch"));
      }
      notification.show?.(
        `invite-${status.toLowerCase()}-${memberId}`,
        status === "ACCEPTED" ? "Convite aceito" : "Convite recusado",
        status === "ACCEPTED" ? "success" : "info",
        status === "ACCEPTED"
          ? "Você agora é membro do projeto."
          : "Convite recusado com sucesso.",
      );
    } catch {
      notification.show?.(
        `invite-error-${memberId}`,
        "Erro",
        "error",
        "Não foi possível processar o convite.",
      );
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <div ref={panelRef} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative flex items-center justify-center w-9 h-9 rounded-lg text-muted-foreground hover:bg-muted transition-colors"
        title="Convites pendentes"
      >
        <Bell size={20} weight="duotone" />
        {invites.length > 0 && (
          <span className="absolute top-1.5 right-1.5 flex items-center justify-center w-4 h-4 text-[9px] font-bold bg-primary text-white rounded-full ring-2 ring-white">
            {invites.length > 9 ? "9+" : invites.length}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-popover rounded-2xl shadow-xl border border-border z-50 overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-border">
            <h3 className="text-sm font-semibold text-foreground">
              Convites pendentes
            </h3>
            <span className="text-xs text-muted-foreground">
              {invites.length} {invites.length === 1 ? "convite" : "convites"}
            </span>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <span className="text-sm text-muted-foreground">Carregando...</span>
              </div>
            ) : invites.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-muted-foreground">
                <FolderOpen size={36} weight="duotone" className="mb-2 opacity-40" />
                <p className="text-sm">Nenhum convite pendente</p>
              </div>
            ) : (
              <ul className="divide-y divide-border">
                {invites.map((invite) => (
                  <li key={invite.kanbanMemberId} className="px-4 py-3">
                    <p className="text-sm font-medium text-foreground truncate">
                      {invite.boardName}
                    </p>
                    {invite.boardDescription && (
                      <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">
                        {invite.boardDescription}
                      </p>
                    )}
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Convidado por{" "}
                      <span className="font-medium text-muted-foreground">
                        @{invite.invitedByLogin}
                      </span>
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <button
                        disabled={processingId === invite.kanbanMemberId}
                        onClick={() => respond(invite.kanbanMemberId, "ACCEPTED")}
                        className="flex items-center gap-1 px-3 py-1 text-xs font-medium text-white bg-primary rounded-lg hover:bg-primary-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <CheckCircle size={13} weight="bold" />
                        Aceitar
                      </button>
                      <button
                        disabled={processingId === invite.kanbanMemberId}
                        onClick={() => respond(invite.kanbanMemberId, "DECLINED")}
                        className="flex items-center gap-1 px-3 py-1 text-xs font-medium text-muted-foreground bg-muted rounded-lg hover:bg-accent-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <XCircle size={13} weight="bold" />
                        Recusar
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
