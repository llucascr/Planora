import { Board, type BoardColumn } from "components";

// const projectsData: Record<string, { name: string; color: string }> = {
//   "1": { name: "Redesign do App", color: "#0E1F63" },
//   "2": { name: "API de Pagamentos", color: "#3d5aad" },
//   "3": { name: "Dashboard Analytics", color: "#16a34a" },
//   "4": { name: "Módulo de Relatórios", color: "#dc2626" },
//   "5": { name: "App Mobile", color: "#0E1F63" },
//   "6": { name: "Portal do Cliente", color: "#3d5aad" },
// };

const initialColumns: BoardColumn[] = [
  {
    id: 1,
    idBoard: 1,
    nome: "Teste 01",
    ordem: 1,
    cards: [
      {
        id: 1,
        codigo: 123,
        createdAt: "2026-03-23T11:57:28.000Z",
        descricao: "teste",
        nome: "teste 01",
        planoAcao: null,
        lead: null,
      },
    ],
  },

  {
    id: 2,
    idBoard: 1,
    nome: "Teste 02",
    ordem: 2,
    cards: [
      {
        id: 2,
        codigo: 123,
        createdAt: "2026-03-20T11:57:28.000Z",
        descricao: "teste",
        nome: "teste 02",
        planoAcao: null,
        lead: null,
      },
    ],
  },
];

export const TarefasPage = () => {
  // const { projectId } = useParams<{ projectId: string }>();

  return (
    <div className="flex flex-col h-full space-y-5">
      <Board columns={initialColumns} />
    </div>
  );
};
