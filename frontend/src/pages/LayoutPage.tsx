import { Outlet } from "react-router-dom";
import { NotificationProvider, UIProvider } from "context";

interface LayoutProps {
  children?: React.ReactNode;
}

export const LayoutPage = ({ children }: LayoutProps) => {
  return (
    <NotificationProvider>
      <UIProvider>
        <div className="flex h-screen overflow-hidden">
          <main className="flex-1 h-full w-full overflow-auto">
            {children}
            <Outlet />
          </main>
        </div>
      </UIProvider>
    </NotificationProvider>
  );
};
