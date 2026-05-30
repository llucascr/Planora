import { config } from "config";
import { useCookie } from "hooks";

export const useAuth = () => {
  const { getCookie } = useCookie();
  return { isAuthenticated: !!getCookie(config.tokenCookieNome) };
};
