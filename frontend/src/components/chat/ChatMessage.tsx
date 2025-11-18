import { motion } from "framer-motion";
import { useState, useEffect } from "react";

interface Props {
    role: "user" | "ai";
    text: string;
}

export default function ChatMessage({ role, text }: Props) {
    const isUser = role === "user";
    const [typed, setTyped] = useState(isUser ? text : "");

    /* 🔥 타이핑 효과 (AI만 적용) */
    useEffect(() => {
        if (isUser) return;

        let i = 0;
        const speed = 14;

        const interval = setInterval(() => {
            i++;
            setTyped(text.slice(0, i));
            if (i >= text.length) clearInterval(interval);
        }, speed);

        return () => clearInterval(interval);
    }, [text, isUser]);

    return (
        <motion.div
            initial={{ opacity: 0, y: 1 }}     // 딱 1px만 이동 → 거의 안 튐
            animate={{ opacity: 1, y: 0 }}
            transition={{
                duration: 0.12,                // 더 짧고 부드럽게
                ease: "easeOut",
            }}
            className={`flex ${isUser ? "justify-end" : "justify-start"} my-1`}
        >
            <div
                className={`max-w-[75%] px-4 py-2 rounded-xl text-sm whitespace-pre-line
                ${
                    isUser
                        ? "bg-blue-500 text-white rounded-br-none"
                        : "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-bl-none"
                }`}
            >
                {typed}
            </div>
        </motion.div>
    );
}
