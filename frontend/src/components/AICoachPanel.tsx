import { motion } from "framer-motion";
import ChatWrapper from "./chat/ChatWrapper";
import ChatContainer from "./chat/ChatContainer";
import { useState } from "react";

interface Props {
    open: boolean;
    onClose: () => void;
}

export default function AICoachPanel({ open, onClose }: Props) {
    const [panelWidth, setPanelWidth] = useState(380);

    return (
        <motion.div
            initial={{ x: "100%" }}
            animate={{ x: open ? 0 : "100%" }}
            transition={{ duration: 0.3, ease: "easeOut" }}
            style={{ width: panelWidth }}
            className="
                fixed top-0 right-0 h-full
                bg-white dark:bg-gray-900 shadow-2xl z-50
                flex flex-col overflow-hidden
            "
        >
            {/* 🔥 1) 패널 좌측 리사이즈 핸들 */}
            <ChatWrapper setWidth={setPanelWidth} />

            {/* 🔥 2) 헤더 */}
            <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
                <h2></h2>
                <h2 className="text-xl font-semibold flex items-center gap-1">
                    <span className="text-[#69A7FF]">Health</span>
                    <span className="text-[#4CCAA1]">Chat+</span>

                </h2>
                <button
                    onClick={onClose}
                    className="text-gray-600 hover:text-black dark:hover:text-white text-2xl"
                >
                    ×
                </button>
            </div>

            {/* 🔥 3) ChatContainer가 반드시 여기 들어가야 함 */}
            <div className="flex-1 overflow-hidden">
                <ChatContainer />
            </div>
        </motion.div>
    );
}
