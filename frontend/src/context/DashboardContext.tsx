import { createContext, useContext, useState } from "react";

interface DashboardContextType {
    shouldRefresh: boolean;
    setShouldRefresh: (v: boolean) => void;

    shouldRefreshFeedback: boolean;
    setShouldRefreshFeedback: (v: boolean) => void;
}

const DashboardContext = createContext<DashboardContextType | null>(null);

export function DashboardProvider({ children }: { children: React.ReactNode }) {
    const [shouldRefresh, setShouldRefresh] = useState(false);
    const [shouldRefreshFeedback, setShouldRefreshFeedback] = useState(false);

    return (
        <DashboardContext.Provider
            value={{
                shouldRefresh,
                setShouldRefresh,
                shouldRefreshFeedback,
                setShouldRefreshFeedback,
            }}
        >
            {children}
        </DashboardContext.Provider>
    );
}

export function useDashboard() {
    const ctx = useContext(DashboardContext);
    if (!ctx) throw new Error("DashboardContext is missing!");
    return ctx;
}
