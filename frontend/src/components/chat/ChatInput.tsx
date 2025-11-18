import { useState, useRef, useEffect } from "react";
import { Send } from "lucide-react";
import { RiBook2Line } from "react-icons/ri";
import { FiTrash2 } from "react-icons/fi";

interface Props {
    onSend: (text: string) => void;
    disabled?: boolean;

    onOpenManual: () => void;
    onClearChat: () => void;
}

export default function ChatInput({ onSend, disabled, onOpenManual, onClearChat }: Props) {
    const [input, setInput] = useState("");
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    /* 🔥 textarea 자동 높이 */
    useEffect(() => {
        const ta = textareaRef.current;
        if (ta) {
            ta.style.height = "auto";
            ta.style.height = ta.scrollHeight + "px";
        }
    }, [input]);

    /* Enter 단독 입력 시 전송 */
    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            if (input.trim()) {
                onSend(input.trim());
                setInput("");
            }
        }
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (input.trim()) {
            onSend(input.trim());
            setInput("");
        }
    };

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-2">

            {/* 🔹 1줄: 입력창 + 전송 버튼 */}
            <div className="flex items-end gap-2">
                <textarea
                    ref={textareaRef}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={disabled}
                    rows={1}
                    placeholder="오늘 먹은 것, 운동, 감정을 입력해보세요..."
                    className="
                        flex-1 resize-none overflow-hidden
                        p-2 text-sm rounded-lg
                        bg-gray-100 dark:bg-gray-700
                        text-gray-900 dark:text-gray-100
                        focus:outline-none focus:ring-2 focus:ring-blue-400
                        max-h-40
                    "
                />

                {/* 📤 전송 버튼 */}
                <button
                    type="submit"
                    disabled={disabled}
                    className="p-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600
                               transition disabled:opacity-50"
                >
                    <Send size={18} />
                </button>
            </div>

            {/* 🔹 2줄: 메뉴얼 + 삭제 */}
            <div className="flex gap-2">

                {/* 📘 메뉴얼 */}
                <button
                    type="button"
                    onClick={onOpenManual}
                    className="p-2 flex-1 rounded-lg bg-gray-200 hover:bg-gray-300
                               dark:bg-gray-600 dark:hover:bg-gray-500
                               transition text-gray-700 dark:text-gray-100"
                >
                    <RiBook2Line size={20} className="mx-auto" />
                </button>

                {/* 🗑 삭제 */}
                <button
                    type="button"
                    onClick={onClearChat}
                    className="p-2 flex-1 rounded-lg bg-red-500 hover:bg-red-600
                               text-white transition"
                >
                    <FiTrash2 size={18} className="mx-auto" />
                </button>
            </div>
        </form>
    );
}
