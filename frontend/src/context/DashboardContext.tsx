import { createContext, useContext, useState } from "react";

interface DashboardContextType {
    shouldRefresh: boolean;
    setShouldRefresh: (v: boolean) => void;
}

const DashboardContext = createContext<DashboardContextType | null>(null);

export function DashboardProvider({ children }: { children: React.ReactNode }) {
    const [shouldRefresh, setShouldRefresh] = useState(false);

    return (
        <DashboardContext.Provider value={{ shouldRefresh, setShouldRefresh }}>
            {children}
        </DashboardContext.Provider>
    );
}

export function useDashboard() {
    const ctx = useContext(DashboardContext);
    if (!ctx) throw new Error("DashboardContext is missing!");
    return ctx;
}
