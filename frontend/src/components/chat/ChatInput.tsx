import { useState, useRef, useEffect } from "react";
import { Send } from "lucide-react";

interface Props {
    onSend: (text: string) => void;
    disabled?: boolean;
}

export default function ChatInput({ onSend, disabled }: Props) {
    const [input, setInput] = useState("");
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    /* 🔥 textarea 자동 높이 조절 */
    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = "auto"; // 초기화
            textarea.style.height = textarea.scrollHeight + "px"; // 내용만큼 증가
        }
    }, [input]);

    /* 🔥 Enter = 줄바꿈 / Shift + Enter = 전송 */
    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault(); // 기본 전송 막음
            if (input.trim() !== "") {
                onSend(input.trim());
                setInput("");
            }
        }
    };

    /* 버튼 클릭 시 전송 */
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (input.trim()) {
            onSend(input.trim());
            setInput("");
        }
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="flex items-end gap-2 border-t border-gray-300 dark:border-gray-700 pt-2"
        >
            {/* 🔥 textarea로 변경 */}
            <textarea
                ref={textareaRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="오늘 먹은 것, 운동, 감정을 입력해보세요..."
                disabled={disabled}
                rows={1}
                className="
                    flex-1 resize-none overflow-hidden
                    p-2 text-sm rounded-lg
                    bg-gray-100 dark:bg-gray-700
                    text-gray-900 dark:text-gray-100
                    focus:outline-none focus:ring-2 focus:ring-blue-400
                    max-h-40
                "
            />

            <button
                type="submit"
                disabled={disabled}
                className="p-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition disabled:opacity-50"
            >
                <Send size={18} />
            </button>
        </form>
    );
}
