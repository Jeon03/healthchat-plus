interface Props {
    role: "user" | "ai";
    text: string;
}

export default function ChatMessage({ role, text }: Props) {
    const isUser = role === "user";
    return (
        <div
            className={`flex ${
                isUser ? "justify-end" : "justify-start"
            }`}
        >
            <div
                className={`max-w-[75%] px-4 py-2 rounded-xl text-sm whitespace-pre-line
                ${isUser
                    ? "bg-blue-500 text-white rounded-br-none"
                    : "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-bl-none"}`}
            >
                {text}
            </div>
        </div>
    );
}
