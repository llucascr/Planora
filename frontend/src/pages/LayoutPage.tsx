import { useEffect, useRef, useState } from "react";
import { Outlet, NavLink, useLocation, useNavigate } from "react-router-dom";
import { NotificationProvider, UIProvider, useUI } from "context";
import { useCookie } from "hooks";
import { config } from "config";
import logo from "../img/logo_horizontal_white.png";
import logoSmall from "../img/logo_solo_white.png";
import {
  House,
  FolderOpen,
  Gear,
  List,
  X,
  CaretRight,
  User,
  SignOut,
  SignIn,
  Sparkle,
} from "@phosphor-icons/react";
import { AiJobsSidebar } from "./Tarefas/AiJobsSidebar";
import { PendingInvitesPanel } from "../components/PendingInvitesPanel";

const BoardAiButton = () => {
  const location = useLocation();
  const ui = useUI();

  const match = location.pathname.match(/^\/projetos\/(\d+)\/tarefas$/);
  if (!match) return null;

  const boardId = Number(match[1]);

  function openAiSidebar() {
    ui.show({
      id: "ai-jobs-sidebar",
      type: "sidebar",
      options: {
        titulo: "Assistente de IA",
        position: "right",
        widthFraction: "1/3",
      },
      content: <AiJobsSidebar boardId={boardId} />,
    });
  }

  return (
    <div className="fixed bottom-6 right-6 z-50">
      <button
        onClick={openAiSidebar}
        className="w-14 h-14 rounded-full shadow-lg flex items-center justify-center bg-linear-to-br from-primary to-[#3d5aad] hover:scale-110 transition-all duration-200"
        title="Assistente de IA"
      >
        <Sparkle size={26} weight="fill" className="text-white" />
      </button>
    </div>
  );
};

interface LayoutProps {
  children?: React.ReactNode;
}

interface NavItem {
  to: string;
  label: string;
  icon: React.ElementType;
  exact?: boolean;
}

const navItems: NavItem[] = [
  { to: "/", label: "Dashboard", icon: House, exact: true },
  { to: "/projetos", label: "Projetos", icon: FolderOpen },
];

const bottomItems: NavItem[] = [
  { to: "/settings", label: "Configurações", icon: Gear },
];

export const LayoutPage = ({ children }: LayoutProps) => {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);
  const { getCookie, deleteCookie } = useCookie();
  const navigate = useNavigate();
  const location = useLocation();

  const [isLoggedIn, setIsLoggedIn] = useState(
    () => !!getCookie(config.tokenCookieNome),
  );

  const handleLogout = () => {
    deleteCookie(config.tokenCookieNome);
    setIsLoggedIn(false);
    setProfileMenuOpen(false);
    navigate("/login");
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        profileMenuRef.current &&
        !profileMenuRef.current.contains(e.target as Node)
      ) {
        setProfileMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    setIsLoggedIn(!!getCookie(config.tokenCookieNome));
  }, [location.pathname]);

  useEffect(() => {
    const link =
      document.querySelector<HTMLLinkElement>("link[rel~='icon']") ??
      document.createElement("link");
    link.rel = "icon";
    link.href = logoSmall;
    document.head.appendChild(link);
  }, []);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  const pageTitle =
    [...navItems, ...bottomItems].find((item) =>
      item.exact
        ? location.pathname === item.to
        : location.pathname.startsWith(item.to) && item.to !== "/",
    )?.label ?? "Dashboard";

  return (
    <NotificationProvider>
      <UIProvider>
        <div className="flex h-screen bg-[#eef0f7] overflow-hidden">
          {/* Mobile overlay */}
          {mobileOpen && (
            <div
              className="fixed inset-0 z-20 bg-black/50 lg:hidden"
              onClick={() => setMobileOpen(false)}
            />
          )}

          {/* Sidebar */}
          <aside
            className={[
              "fixed lg:relative z-30 flex flex-col h-full bg-linear-to-b from-primary to-[#091550] text-white shadow-2xl overflow-hidden",
              "transition-[width] duration-300 ease-in-out",
              collapsed ? "w-20" : "w-60",
              mobileOpen
                ? "translate-x-0"
                : "-translate-x-full lg:translate-x-0",
            ].join(" ")}
          >
            {/* Logo */}
            <div className="flex items-center border-b border-white/10 px-3 py-4 shrink-0">
              <div
                className="relative flex-1 flex items-center justify-center overflow-hidden"
                style={{ height: 48 }}
              >
                {/* Logo expandido */}
                <img
                  src={logo}
                  alt="Planora"
                  className={[
                    "absolute h-12 object-contain transition-all duration-300",
                    collapsed
                      ? "opacity-0 scale-90 pointer-events-none"
                      : "opacity-100 scale-100",
                  ].join(" ")}
                />
                {/* Logo recolhido */}
                <img
                  src={logoSmall}
                  alt="Planora"
                  className={[
                    "absolute h-10 object-contain transition-all duration-300",
                    collapsed
                      ? "opacity-100 scale-100"
                      : "opacity-0 scale-90 pointer-events-none",
                  ].join(" ")}
                />
              </div>
              <button
                onClick={() => setCollapsed((c) => !c)}
                className="hidden lg:flex items-center justify-center w-7 h-7 rounded-md text-white/60 hover:text-white hover:bg-white/10 transition-colors shrink-0"
                title={collapsed ? "Expandir menu" : "Recolher menu"}
              >
                <CaretRight
                  size={14}
                  weight="bold"
                  className={[
                    "transition-transform duration-300",
                    collapsed ? "" : "rotate-180",
                  ].join(" ")}
                />
              </button>
            </div>

            {/* Nav items */}
            <nav className="flex-1 overflow-y-auto py-4 space-y-1 px-2">
              {navItems.map(({ to, label, icon: Icon, exact }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={exact}
                  className={({ isActive }) =>
                    [
                      "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150",
                      isActive
                        ? "bg-white/20 text-white shadow-inner"
                        : "text-white/70 hover:bg-white/10 hover:text-white",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  <Icon size={22} weight="duotone" className="shrink-0" />
                  <span
                    className={[
                      "truncate transition-all duration-300 whitespace-nowrap",
                      collapsed
                        ? "opacity-0 w-0 overflow-hidden"
                        : "opacity-100 w-auto",
                    ].join(" ")}
                  >
                    {label}
                  </span>
                </NavLink>
              ))}
            </nav>

            {/* Bottom items */}
            <div className="border-t border-white/10 py-4 space-y-1 px-2">
              {bottomItems.map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    [
                      "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150",
                      isActive
                        ? "bg-white/20 text-white"
                        : "text-white/70 hover:bg-white/10 hover:text-white",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  <Icon size={22} weight="duotone" className="shrink-0" />
                  <span
                    className={[
                      "truncate transition-all duration-300 whitespace-nowrap",
                      collapsed
                        ? "opacity-0 w-0 overflow-hidden"
                        : "opacity-100 w-auto",
                    ].join(" ")}
                  >
                    {label}
                  </span>
                </NavLink>
              ))}

              {/* User profile / login */}
              {isLoggedIn ? (
                <div ref={profileMenuRef} className="relative mt-2">
                  <div
                    onClick={() => setProfileMenuOpen((o) => !o)}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-white/10 cursor-pointer hover:bg-white/20 transition-colors"
                    title={collapsed ? "Meu perfil" : undefined}
                  >
                    <div className="w-7 h-7 rounded-full bg-[#3d5aad] flex items-center justify-center shrink-0">
                      <User size={16} weight="fill" className="text-white" />
                    </div>
                    <div
                      className={[
                        "min-w-0 transition-all duration-300",
                        collapsed
                          ? "opacity-0 w-0 overflow-hidden"
                          : "opacity-100 w-auto",
                      ].join(" ")}
                    >
                      <p className="text-sm font-medium text-white truncate whitespace-nowrap">
                        Meu Perfil
                      </p>
                      <p className="text-xs text-white/50 truncate whitespace-nowrap">
                        Ver conta
                      </p>
                    </div>
                  </div>

                  {profileMenuOpen && (
                    <div className="absolute bottom-full left-0 mb-1 w-full bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden z-50">
                      <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 w-full px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <SignOut size={16} weight="bold" />
                        Sair da conta
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <button
                  onClick={() => navigate("/login")}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-lg mt-2 w-full bg-white/10 hover:bg-white/20 transition-colors"
                  title={collapsed ? "Entrar" : undefined}
                >
                  <div className="w-7 h-7 rounded-full bg-[#3d5aad] flex items-center justify-center shrink-0">
                    <SignIn size={16} weight="fill" className="text-white" />
                  </div>
                  <span
                    className={[
                      "text-sm font-medium text-white transition-all duration-300 whitespace-nowrap",
                      collapsed
                        ? "opacity-0 w-0 overflow-hidden"
                        : "opacity-100 w-auto",
                    ].join(" ")}
                  >
                    Entrar
                  </span>
                </button>
              )}
            </div>
          </aside>

          {/* Main area */}
          <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
            {/* Topbar */}
            <header className="flex items-center justify-between h-14 px-4 bg-white border-b border-gray-200 shadow-sm shrink-0">
              <div className="flex items-center gap-3">
                {/* Mobile hamburger */}
                <button
                  onClick={() => setMobileOpen((o) => !o)}
                  className="lg:hidden flex items-center justify-center w-8 h-8 rounded-md text-gray-500 hover:bg-gray-100 transition-colors"
                >
                  {mobileOpen ? <X size={20} /> : <List size={20} />}
                </button>

                {/* Breadcrumb / page title */}
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  <span className="hidden sm:inline">Planora</span>
                  <span className="hidden sm:inline text-gray-300">/</span>
                  <span className="font-semibold text-gray-800">
                    {pageTitle}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <PendingInvitesPanel />
              </div>
            </header>

            {/* Content */}
            <main className="flex-1 overflow-auto p-6">
              {children}
              <Outlet />
            </main>
          </div>

          <BoardAiButton />
        </div>
      </UIProvider>
    </NotificationProvider>
  );
};
