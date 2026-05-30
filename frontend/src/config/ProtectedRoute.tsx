import { Outlet } from "react-router-dom";
import { useAuth } from "hooks";

export const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Outlet /> : (window.location.href = "/login");
};

export const PublicOnlyRoute = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? (window.location.href = "/login") : <Outlet />;
};
