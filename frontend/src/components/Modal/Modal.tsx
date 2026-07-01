import { X } from "@phosphor-icons/react";
import type { ModalOptions, UIComponentProps } from "types";

const SIZE_CLASS: Record<NonNullable<ModalOptions["size"]>, string> = {
  small: "max-w-md",
  medium: "max-w-lg",
  large: "max-w-2xl",
};

export const Modal = ({
  isOpen,
  onClose,
  options,
  children,
}: UIComponentProps) => {
  if (!isOpen) return null;

  const opt = options as ModalOptions | undefined;
  const sizeClass = SIZE_CLASS[opt?.size ?? "medium"];
  // Quando aberto junto de um drawer lateral (1/3), desloca o modal para a
  // esquerda para não encostar no painel.
  const padClass = opt?.sidebarOffset ? "p-4 lg:pr-[36vw]" : "p-4";

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-overlay-in ${padClass}`}
      onClick={onClose} // Fecha modal clicando fora do conteúdo
    >
      <div
        className={`bg-card rounded-2xl shadow-2xl w-full ${sizeClass} max-h-[90vh] overflow-y-auto p-6 relative animate-modal-in`}
        onClick={(e) => e.stopPropagation()} // Impede fechar ao clicar dentro do modal
      >
        <div className="flex items-center justify-between mb-4 relative pr-10">
          <h1 className="text-foreground text-xl font-bold tracking-tight">
            {opt?.titulo ?? ""}
          </h1>
          <button
            className="absolute -top-1 right-0 flex items-center justify-center w-8 h-8 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            <X size={18} weight="bold" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
};
