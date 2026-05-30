import {
  ErrorPage,
  HomePage,
  LayoutPage,
  LoginPage,
  ProjetosPage,
  TarefasPage,
  Callback,
} from "pages";
import { createBrowserRouter } from "react-router-dom";
import { ProtectedRoute, PublicOnlyRoute } from "./ProtectedRoute";

export const Router = createBrowserRouter([
  {
    id: "root",
    path: "/",
    Component: LayoutPage,
    loader: ProtectedRoute,
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
