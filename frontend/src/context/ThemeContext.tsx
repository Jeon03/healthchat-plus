import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react"; // ✅ 타입 전용 import

interface ThemeContextType {
    theme: "light" | "dark";
    toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<"light" | "dark">(() => {
        if (typeof window !== "undefined") {
            return (localStorage.getItem("theme") as "light" | "dark") ||
                (window.matchMedia("(prefers-color-scheme: dark)").matches
                    ? "dark"
                    : "light");
        }
        return "light";
    });

    useEffect(() => {
        const root = window.document.documentElement;
        root.classList.remove(theme === "light" ? "dark" : "light");
        root.classList.add(theme);
        localStorage.setItem("theme", theme);
    }, [theme]);

    const toggleTheme = () => setTheme((prev) => (prev === "light" ? "dark" : "light"));

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme() {
    const context = useContext(ThemeContext);
    if (!context) throw new Error("useTheme must be used within ThemeProvider");
    return context;
}
