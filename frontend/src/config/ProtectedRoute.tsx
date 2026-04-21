import { Navigate, Outlet } from "react-router-dom";
import { useCookie } from "hooks";
import { config } from "./config";

export const ProtectedRoute = () => {
  const { getCookie } = useCookie();
  const isLoggedIn = !!getCookie(config.tokenCookieNome);

  return isLoggedIn ? <Outlet /> : <Navigate to="/login" replace />;
};

export const PublicOnlyRoute = () => {
  const { getCookie } = useCookie();
  const isLoggedIn = !!getCookie(config.tokenCookieNome);

  return isLoggedIn ? <Navigate to="/" replace /> : <Outlet />;
};
