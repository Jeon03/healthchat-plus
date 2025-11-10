import { useState } from "react";
import { Send } from "lucide-react";

interface Props {
    onSend: (text: string) => void;
    disabled?: boolean;
}

export default function ChatInput({ onSend, disabled }: Props) {
    const [input, setInput] = useState("");

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (input.trim()) {
            onSend(input);
            setInput("");
        }
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="flex items-center gap-2 border-t border-gray-300 pt-2"
        >
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="오늘 먹은 걸 입력해보세요..."
                className="flex-1 p-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                disabled={disabled}
            />
            <button
                type="submit"
                disabled={disabled}
                className="p-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition"
            >
                <Send size={18} />
            </button>
        </form>
    );
}
