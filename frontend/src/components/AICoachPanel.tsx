import { motion } from "framer-motion";
import ChatContainer from "./chat/ChatContainer";

interface Props {
    open: boolean;
    onClose: () => void;
}

export default function AICoachPanel({ open, onClose }: Props) {
    return (
        <>
            {/* 🔹 Right Slide Panel ONLY (오버레이 제거됨) */}
            <motion.div
                initial={{ x: "100%" }}
                animate={{ x: open ? "0%" : "100%" }}
                transition={{ duration: 0.3, ease: "easeOut" }}
                className="fixed top-0 right-0 w-[380px] max-w-[90%] h-full
                           bg-white dark:bg-gray-900 shadow-2xl z-50
                           flex flex-col"
            >
                {/* Header */}
                <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
                    <h2 className="text-xl font-semibold">AI 건강 코치</h2>
                    <button
                        onClick={onClose}
                        className="text-gray-600 hover:text-black dark:hover:text-white text-2xl"
                    >
                        ×
                    </button>
                </div>

                {/* Chat */}
                <div className="flex-1 overflow-hidden">
                    <ChatContainer />
                </div>
            </motion.div>
        </>
    );
}
