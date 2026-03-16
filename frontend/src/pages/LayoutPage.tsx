import { useEffect, useState } from "react";
import { Outlet, NavLink, useLocation } from "react-router-dom";
import { NotificationProvider, UIProvider } from "context";
import logo from "../img/logo_horizontal_white.png";
import logoSmall from "../img/logo_solo_white.png";
import {
  House,
  FolderOpen,
  CheckSquare,
  CalendarBlank,
  ChartBar,
  Gear,
  Bell,
  List,
  X,
  CaretRight,
  User,
} from "@phosphor-icons/react";

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
  const location = useLocation();

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

  const pageTitle = [...navItems, ...bottomItems].find((item) =>
    item.exact
      ? location.pathname === item.to
      : location.pathname.startsWith(item.to) && item.to !== "/"
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
              "fixed lg:relative z-30 flex flex-col h-full bg-gradient-to-b from-[#0E1F63] to-[#091550] text-white transition-all duration-300 ease-in-out shadow-2xl",
              collapsed ? "w-20" : "w-60",
              mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
            ].join(" ")}
          >
            {/* Logo */}
            {collapsed ? (
              <div className="flex flex-col items-center gap-2 px-2 py-4 border-b border-white/10">
                <img src={logoSmall} alt="Planora" className="h-12 object-contain" />
                <button
                  onClick={() => setCollapsed(false)}
                  className="hidden lg:flex items-center justify-center w-7 h-7 rounded-md text-white/60 hover:text-white hover:bg-white/10 transition-colors"
                  title="Expandir menu"
                >
                  <CaretRight size={14} weight="bold" />
                </button>
              </div>
            ) : (
              <div className="flex items-center justify-between px-3 py-5 border-b border-white/10">
                <img src={logo} alt="Planora" className="h-14 object-contain mx-auto" />
                <button
                  onClick={() => setCollapsed(true)}
                  className="hidden lg:flex items-center justify-center w-6 h-6 rounded-md text-white/60 hover:text-white hover:bg-white/10 transition-colors shrink-0"
                  title="Recolher menu"
                >
                  <CaretRight size={14} weight="bold" className="rotate-180" />
                </button>
              </div>
            )}

            {/* Nav items */}
            <nav className="flex-1 overflow-y-auto py-4 space-y-1 px-2">
              {navItems.map(({ to, label, icon: Icon, exact }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={exact}
                  className={({ isActive }) =>
                    [
                      "flex items-center py-2.5 rounded-lg text-sm font-medium transition-all duration-150",
                      collapsed ? "justify-center px-2" : "gap-3 px-3",
                      isActive
                        ? "bg-white/20 text-white shadow-inner"
                        : "text-white/70 hover:bg-white/10 hover:text-white",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  <Icon size={22} weight="duotone" className="shrink-0" />
                  {!collapsed && <span className="truncate">{label}</span>}
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
                      "flex items-center py-2.5 rounded-lg text-sm font-medium transition-all duration-150",
                      collapsed ? "justify-center px-2" : "gap-3 px-3",
                      isActive
                        ? "bg-white/20 text-white"
                        : "text-white/70 hover:bg-white/10 hover:text-white",
                    ].join(" ")
                  }
                  title={collapsed ? label : undefined}
                >
                  <Icon size={22} weight="duotone" className="shrink-0" />
                  {!collapsed && <span className="truncate">{label}</span>}
                </NavLink>
              ))}

              {/* User profile */}
              <div
                className={[
                  "flex items-center gap-3 px-3 py-2.5 rounded-lg mt-2 bg-white/10 cursor-pointer hover:bg-white/20 transition-colors",
                  collapsed ? "justify-center" : "",
                ].join(" ")}
                title={collapsed ? "Meu perfil" : undefined}
              >
                <div className="w-7 h-7 rounded-full bg-[#3d5aad] flex items-center justify-center shrink-0">
                  <User size={16} weight="fill" className="text-white" />
                </div>
                {!collapsed && (
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-white truncate">Meu Perfil</p>
                    <p className="text-xs text-white/50 truncate">Ver conta</p>
                  </div>
                )}
              </div>
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
                  <span className="font-semibold text-gray-800">{pageTitle}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {/* Notifications */}
                <button className="relative flex items-center justify-center w-9 h-9 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors">
                  <Bell size={20} weight="duotone" />
                  <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[#0E1F63] rounded-full ring-2 ring-white" />
                </button>

                {/* Avatar */}
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#0E1F63] to-[#3d5aad] flex items-center justify-center cursor-pointer">
                  <User size={16} weight="fill" className="text-white" />
                </div>
              </div>
            </header>

            {/* Content */}
            <main className="flex-1 overflow-auto p-6">
              {children}
              <Outlet />
            </main>
          </div>

        </div>
      </UIProvider>
    </NotificationProvider>
  );
};
