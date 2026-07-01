import { useEffect, useRef, useState } from "react";
import { Outlet, NavLink, useLocation, useNavigate } from "react-router-dom";
import { NotificationProvider, UIProvider, useUI } from "context";
import { useCookie } from "hooks";
import { config } from "config";
import { httpClient } from "api";
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
  const [hasProcessing, setHasProcessing] = useState(false);

  const match = location.pathname.match(/^\/projetos\/(\d+)\/tarefas$/);
  const boardId = match ? Number(match[1]) : null;

  useEffect(() => {
    if (!boardId) return;

    let mounted = true;

    async function checkJobs() {
      try {
        const jobs = await httpClient.get<any[]>(`/v1/jobs?boardId=${boardId}`);
        if (mounted) {
          setHasProcessing(jobs.some((j) => j.status === "PROCESSING"));
        }
      } catch (err) {
        // ignore
      }
    }

    checkJobs();
    const intervalId = setInterval(checkJobs, 10000); // 10 seconds polling
    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [boardId]);

  if (!boardId) return null;

  function openAiSidebar() {
    ui.show({
      id: "ai-jobs-sidebar",
      type: "sidebar",
      options: {
        titulo: "Assistente de IA",
        position: "right",
        widthFraction: "1/3",
      },
      content: <AiJobsSidebar boardId={boardId!} />,
    });
  }

  return (
    <div className="fixed bottom-6 right-6 z-40">
      <button
        onClick={openAiSidebar}
        className="relative w-14 h-14 rounded-full shadow-lg flex items-center justify-center bg-linear-to-br from-primary to-primary-hover hover:scale-110 transition-all duration-200"
        title="Assistente de IA"
      >
        <Sparkle size={26} weight="fill" className="text-white" />
        {hasProcessing && (
          <span className="absolute top-0 right-0 w-3.5 h-3.5 bg-red-500 border-2 border-background rounded-full animate-pulse shadow-sm" />
        )}
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
        <div className="flex h-screen bg-background overflow-hidden">
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
              "fixed lg:relative z-30 flex flex-col h-full bg-sidebar text-sidebar-foreground border-r border-sidebar-border shadow-sm overflow-hidden",
              "transition-[width] duration-300 ease-in-out",
              collapsed ? "w-20" : "w-64",
              mobileOpen
                ? "translate-x-0"
                : "-translate-x-full lg:translate-x-0",
            ].join(" ")}
          >
            {/* Logo */}
            <div className="flex items-center gap-3 px-4 py-5 shrink-0">
              <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-linear-to-br from-primary to-primary-hover shadow-sm shrink-0">
                <img
                  src={logoSmall}
                  alt="Planora"
                  className="h-6 w-6 object-contain"
                />
              </div>
              <span
                className={[
                  "text-lg font-extrabold tracking-tight text-foreground transition-all duration-300 whitespace-nowrap",
                  collapsed
                    ? "opacity-0 w-0 overflow-hidden"
                    : "opacity-100 w-auto flex-1",
                ].join(" ")}
              >
                Planora
              </span>
              <button
                onClick={() => setCollapsed((c) => !c)}
                className={[
                  "hidden lg:flex items-center justify-center w-7 h-7 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors shrink-0",
                  collapsed ? "hidden" : "",
                ].join(" ")}
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
            <nav className="flex-1 overflow-y-auto py-4 space-y-1 px-3">
              {navItems.map(({ to, label, icon: Icon, exact }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={exact}
                  className={({ isActive }) =>
                    [
                      "flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-colors duration-150",
                      isActive
                        ? "bg-sidebar-accent text-sidebar-accent-foreground font-semibold"
                        : "text-muted-foreground font-medium hover:bg-muted hover:text-foreground",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  {({ isActive }) => (
                    <>
                      <Icon
                        size={20}
                        weight={isActive ? "fill" : "regular"}
                        className="shrink-0"
                      />
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
                    </>
                  )}
                </NavLink>
              ))}
            </nav>

            {/* Bottom items */}
            <div className="py-4 space-y-1 px-3">
              {bottomItems.map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    [
                      "flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-colors duration-150",
                      isActive
                        ? "bg-sidebar-accent text-sidebar-accent-foreground font-semibold"
                        : "text-muted-foreground font-medium hover:bg-muted hover:text-foreground",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  {({ isActive }) => (
                    <>
                      <Icon
                        size={20}
                        weight={isActive ? "fill" : "regular"}
                        className="shrink-0"
                      />
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
                    </>
                  )}
                </NavLink>
              ))}

              {/* User profile / login */}
              {isLoggedIn ? (
                <div ref={profileMenuRef} className="relative mt-2">
                  <div
                    onClick={() => setProfileMenuOpen((o) => !o)}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-sidebar-accent cursor-pointer hover:bg-accent-hover transition-colors"
                    title={collapsed ? "Meu perfil" : undefined}
                  >
                    <div className="w-9 h-9 rounded-lg bg-linear-to-br from-primary to-primary-hover flex items-center justify-center shrink-0">
                      <User size={18} weight="fill" className="text-white" />
                    </div>
                    <div
                      className={[
                        "min-w-0 transition-all duration-300",
                        collapsed
                          ? "opacity-0 w-0 overflow-hidden"
                          : "opacity-100 w-auto",
                      ].join(" ")}
                    >
                      <p className="text-sm font-semibold text-foreground truncate whitespace-nowrap">
                        Meu Perfil
                      </p>
                      <p className="text-xs text-muted-foreground truncate whitespace-nowrap">
                        Ver conta
                      </p>
                    </div>
                  </div>

                  {profileMenuOpen && (
                    <div className="absolute bottom-full left-0 mb-1 w-full bg-popover rounded-xl shadow-lg border border-border overflow-hidden z-50">
                      <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 w-full px-4 py-2.5 text-sm text-destructive hover:bg-destructive/10 transition-colors"
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
                  className="flex items-center gap-3 px-3 py-2.5 rounded-xl mt-2 w-full bg-sidebar-accent hover:bg-accent-hover transition-colors"
                  title={collapsed ? "Entrar" : undefined}
                >
                  <div className="w-9 h-9 rounded-lg bg-linear-to-br from-primary to-primary-hover flex items-center justify-center shrink-0">
                    <SignIn size={18} weight="fill" className="text-white" />
                  </div>
                  <span
                    className={[
                      "text-sm font-semibold text-foreground transition-all duration-300 whitespace-nowrap",
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
            <header className="flex items-center justify-between h-16 px-6 bg-card border-b border-border shrink-0">
              <div className="flex items-center gap-3">
                {/* Mobile hamburger */}
                <button
                  onClick={() => setMobileOpen((o) => !o)}
                  className="lg:hidden flex items-center justify-center w-8 h-8 rounded-md text-muted-foreground hover:bg-muted transition-colors"
                >
                  {mobileOpen ? <X size={20} /> : <List size={20} />}
                </button>

                {/* Breadcrumb / page title */}
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <span className="hidden sm:inline">Planora</span>
                  <span className="hidden sm:inline text-border">/</span>
                  <span className="font-semibold text-foreground">
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
