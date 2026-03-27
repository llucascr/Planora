import { useEffect, useRef, useState } from "react";
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
  ChatCircleDots,
  PaperPlaneTilt,
  Robot,
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

type ChatMessage = { from: "user" | "bot"; text: string };

const BOT_REPLIES: Record<string, string> = {
  default: "Entendido! Posso te ajudar com informações sobre seus projetos. Qual é a sua dúvida?",
  oi: "Olá! 👋 Sou o assistente da Planora. Como posso te ajudar com seus projetos?",
  ajuda: "Claro! Posso te ajudar a entender o andamento dos projetos, prazos e tarefas. O que você precisa saber?",
  prazo: "Os projetos com prazo mais próximo são: **Portal do Cliente** (25/03) e **Redesign do App** (30/03).",
  atrasado: "Atualmente o projeto **Módulo de Relatórios** está atrasado. O prazo era 10/03 e o progresso está em 20%.",
};

function getBotReply(msg: string): string {
  const lower = msg.toLowerCase();
  for (const key of Object.keys(BOT_REPLIES)) {
    if (lower.includes(key)) return BOT_REPLIES[key];
  }
  return BOT_REPLIES.default;
}

export const LayoutPage = ({ children }: LayoutProps) => {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { from: "bot", text: "Olá! 👋 Sou o assistente da Planora. Como posso te ajudar com seus projetos?" },
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const location = useLocation();

  const isProjetosPage = location.pathname.startsWith("/projetos");

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  function sendMessage() {
    const text = input.trim();
    if (!text) return;
    setMessages((prev) => [...prev, { from: "user", text }]);
    setInput("");
    setIsTyping(true);
    setTimeout(() => {
      setIsTyping(false);
      setMessages((prev) => [...prev, { from: "bot", text: getBotReply(text) }]);
    }, 900);
  }

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
              "fixed lg:relative z-30 flex flex-col h-full bg-gradient-to-b from-[#0E1F63] to-[#091550] text-white shadow-2xl overflow-hidden",
              "transition-[width] duration-300 ease-in-out",
              collapsed ? "w-20" : "w-60",
              mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
            ].join(" ")}
          >
            {/* Logo */}
            <div className="flex items-center border-b border-white/10 px-3 py-4 shrink-0">
              <div className="relative flex-1 flex items-center justify-center overflow-hidden" style={{ height: 48 }}>
                {/* Logo expandido */}
                <img
                  src={logo}
                  alt="Planora"
                  className={[
                    "absolute h-12 object-contain transition-all duration-300",
                    collapsed ? "opacity-0 scale-90 pointer-events-none" : "opacity-100 scale-100",
                  ].join(" ")}
                />
                {/* Logo recolhido */}
                <img
                  src={logoSmall}
                  alt="Planora"
                  className={[
                    "absolute h-10 object-contain transition-all duration-300",
                    collapsed ? "opacity-100 scale-100" : "opacity-0 scale-90 pointer-events-none",
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
                  className={["transition-transform duration-300", collapsed ? "" : "rotate-180"].join(" ")}
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
                      collapsed ? "opacity-0 w-0 overflow-hidden" : "opacity-100 w-auto",
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
                      collapsed ? "opacity-0 w-0 overflow-hidden" : "opacity-100 w-auto",
                    ].join(" ")}
                  >
                    {label}
                  </span>
                </NavLink>
              ))}

              {/* User profile */}
              <div
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg mt-2 bg-white/10 cursor-pointer hover:bg-white/20 transition-colors"
                title={collapsed ? "Meu perfil" : undefined}
              >
                <div className="w-7 h-7 rounded-full bg-[#3d5aad] flex items-center justify-center shrink-0">
                  <User size={16} weight="fill" className="text-white" />
                </div>
                <div
                  className={[
                    "min-w-0 transition-all duration-300",
                    collapsed ? "opacity-0 w-0 overflow-hidden" : "opacity-100 w-auto",
                  ].join(" ")}
                >
                  <p className="text-sm font-medium text-white truncate whitespace-nowrap">Meu Perfil</p>
                  <p className="text-xs text-white/50 truncate whitespace-nowrap">Ver conta</p>
                </div>
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

              </div>
            </header>

            {/* Content */}
            <main className="flex-1 overflow-auto p-6">
              {children}
              <Outlet />
            </main>
          </div>

          {/* Chat FAB — só aparece em /projetos */}
          {isProjetosPage && (
            <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">

              {/* Chat panel */}
              {chatOpen && (
                <div className="w-80 h-[420px] bg-white rounded-2xl shadow-2xl border border-gray-100 flex flex-col overflow-hidden animate-[fadeSlideUp_0.2s_ease-out]">
                  {/* Header */}
                  <div className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-[#0E1F63] to-[#3d5aad]">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
                        <Robot size={16} weight="fill" className="text-white" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-white leading-tight">Assistente</p>
                        <p className="text-[10px] text-white/60">Planora AI</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setChatOpen(false)}
                      className="w-7 h-7 flex items-center justify-center rounded-lg text-white/60 hover:text-white hover:bg-white/10 transition-colors"
                    >
                      <X size={16} weight="bold" />
                    </button>
                  </div>

                  {/* Messages */}
                  <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3 bg-[#f8f9fc]">
                    {messages.map((msg, i) => (
                      <div key={i} className={`flex ${msg.from === "user" ? "justify-end" : "justify-start"}`}>
                        {msg.from === "bot" && (
                          <div className="w-6 h-6 rounded-full bg-[#0E1F63] flex items-center justify-center mr-2 mt-0.5 shrink-0">
                            <Robot size={12} weight="fill" className="text-white" />
                          </div>
                        )}
                        <div
                          className={[
                            "max-w-[80%] px-3 py-2 rounded-2xl text-sm leading-relaxed",
                            msg.from === "user"
                              ? "bg-[#0E1F63] text-white rounded-br-sm"
                              : "bg-white text-gray-700 shadow-sm border border-gray-100 rounded-bl-sm",
                          ].join(" ")}
                        >
                          {msg.text}
                        </div>
                      </div>
                    ))}
                    {isTyping && (
                      <div className="flex justify-start">
                        <div className="w-6 h-6 rounded-full bg-[#0E1F63] flex items-center justify-center mr-2 mt-0.5 shrink-0">
                          <Robot size={12} weight="fill" className="text-white" />
                        </div>
                        <div className="bg-white border border-gray-100 shadow-sm px-4 py-3 rounded-2xl rounded-bl-sm flex gap-1 items-center">
                          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:0ms]" />
                          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:150ms]" />
                          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:300ms]" />
                        </div>
                      </div>
                    )}
                    <div ref={messagesEndRef} />
                  </div>

                  {/* Input */}
                  <div className="px-3 py-3 bg-white border-t border-gray-100 flex items-center gap-2">
                    <input
                      type="text"
                      placeholder="Digite uma mensagem..."
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      onKeyDown={(e) => e.key === "Enter" && sendMessage()}
                      className="flex-1 text-sm px-3 py-2 rounded-xl bg-gray-50 border border-gray-200 outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
                    />
                    <button
                      onClick={sendMessage}
                      disabled={!input.trim()}
                      className="w-9 h-9 rounded-xl bg-[#0E1F63] flex items-center justify-center text-white hover:bg-[#1a2f7a] transition-colors disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
                    >
                      <PaperPlaneTilt size={16} weight="fill" />
                    </button>
                  </div>
                </div>
              )}

              {/* FAB button */}
              <button
                onClick={() => setChatOpen((o) => !o)}
                className={[
                  "w-14 h-14 rounded-full shadow-lg flex items-center justify-center transition-all duration-200",
                  chatOpen
                    ? "bg-gray-700 hover:bg-gray-800 rotate-0"
                    : "bg-gradient-to-br from-[#0E1F63] to-[#3d5aad] hover:scale-110",
                ].join(" ")}
                title="Abrir assistente"
              >
                {chatOpen
                  ? <X size={22} weight="bold" className="text-white" />
                  : <ChatCircleDots size={26} weight="fill" className="text-white" />
                }
              </button>
            </div>
          )}

        </div>
      </UIProvider>
    </NotificationProvider>
  );
};
