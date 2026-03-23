import type { Card } from "../domain/types";
import moment from "moment";

interface CardGenericProps {
  card: Card;
  compact?: boolean;
  onClick?: () => void;
}

export function CardGeneric({ card, onClick }: CardGenericProps) {
  const date = card.createdAt
    ? moment(new Date(card.createdAt)).locale("pt-br").format("DD MMM")
    : null;

  return (
    <div onClick={onClick} className="flex flex-col gap-1.5">
      <p className="text-[13px] font-semibold text-foreground leading-snug">
        {card.nome || "Card sem título"}
      </p>
      {card.descricao && (
        <p className="text-[11px] text-muted-foreground line-clamp-2 leading-relaxed">
          {card.descricao}
        </p>
      )}
      {date && (
        <p className="text-[10px] text-muted-foreground mt-0.5">{date}</p>
      )}
    </div>
  );
}
