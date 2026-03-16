import { ErrorPage, HomePage, LayoutPage, LoginPage, ProjetosPage, TarefasPage, ChatbotPage } from "pages"; 
import { createBrowserRouter } from "react-router-dom";

export const Router = createBrowserRouter([
  {
    id: "root",
    path: "/",
    Component: LayoutPage, 
    children: [
      {
        path: "/",
        Component: HomePage,
        errorElement: <ErrorPage />,
      },
      {
        id: "projetos",
        path: "/projetos",
        Component: ProjetosPage,
      },
      {
        id: "tarefas",
        path: "/tarefas",
        Component: TarefasPage,
      },
      {
        id: "chatbot",
        path: "/chatbot",
        Component: ChatbotPage,
      }
    ],
    errorElement: <ErrorPage />,
  },
  {
    id: "login",
    path: "/login",
    Component: LoginPage,
  },
  
]);
