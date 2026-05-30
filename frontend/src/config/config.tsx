import type { ConfigType } from "types";

/**
 * Configurações estáticas da aplicação.
 */
export const config: ConfigType = {
  nomeFantasia: "Planora",
  apiUrl: import.meta.env.VITE_API_URL,
  aplicacaoUrl: import.meta.env.VITE_URL,
  tokenCookieNome: import.meta.env.VITE_TOKEN_COOKIE_NAME ?? "token",

  logo: "URL/SRC",

  UIContext: "modal",
  UIOptions: {
    position: "right",
    size: "medium",
    titulo: "Modal Padrão",
    widthFraction: "1/3",
  },
};
