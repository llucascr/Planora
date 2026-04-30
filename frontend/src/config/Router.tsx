import { ErrorPage, HomePage, LayoutPage, LoginPage, ProjetosPage, TarefasPage, ChatbotPage, Callback } from "pages";
import { createBrowserRouter } from "react-router-dom";
import { ProtectedRoute, PublicOnlyRoute } from "./ProtectedRoute";

export const Router = createBrowserRouter([
  {
    loader: ProtectedRoute,
    children: [
      {
        id: "root",
        path: "/",
        Component: LayoutPage,
        errorElement: <ErrorPage />,
        children: [
          {
            path: "/",
            Component: HomePage,
          },
          {
            id: "projetos",
            path: "/projetos",
            Component: ProjetosPage,
          },
          {
            id: "tarefas",
            path: "/projetos/:projectId/tarefas",
            Component: TarefasPage,
          },
          {
            id: "chatbot",
            path: "/chatbot",
            Component: ChatbotPage,
          },
        ],
      },
    ],
  },
  {
    Component: PublicOnlyRoute,
    children: [
      {
        id: "login",
        path: "/login",
        Component: LoginPage,
      },
    ],
  },
  {
    id: "callback",
    path: "/callback/:token",
    Component: Callback,
  },
  {
    path: "*",
    element: <ErrorPage />,
  },
]);
