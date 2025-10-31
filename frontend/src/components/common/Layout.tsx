import type { ReactNode } from "react";
import Header from "./Header.tsx";
import Footer from "./Footer.tsx";

/**
 * LayoutProps 타입 정의 — children은 React 컴포넌트를 의미
 */
interface LayoutProps {
    children: ReactNode;
}

/**
 * Layout 컴포넌트
 * - 모든 페이지에 Header / Footer를 공통 적용
 * - 다크모드 대응 (Tailwind의 dark class 기반)
 */
export default function Layout({ children }: LayoutProps) {
    return (
        <div className="min-h-screen flex flex-col bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 transition-colors duration-300">
            <Header />

            <main className="flex-1 mt-16 px-4 sm:px-6 lg:px-8">{children}</main>

            <Footer />
        </div>
    );
}
