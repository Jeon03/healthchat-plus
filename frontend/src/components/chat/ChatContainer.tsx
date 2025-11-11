import {useState} from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import api from "../../api/axios";

type ChatRole = "user" | "ai";

interface FoodItem {
    name: string;
    quantity: number;
    unit: string;
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
}

interface Meal {
    time: string;
    foods: FoodItem[];
}

interface DailyAnalysis {
    action?: "add" | "update" | "delete";
    targetMeal?: string;
    meals: Meal[];
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
}

interface Message {
    role: ChatRole;
    text: string;
}

export default function ChatContainer() {
    const [messages, setMessages] = useState<Message[]>([
        { role: "ai", text: "안녕하세요 👋 오늘의 식단을 기록해볼까요?" },
    ]);
    const [loading, setLoading] = useState(false);

    const handleSend = async (userText: string) => {
        if (!userText.trim()) return;

        const newMessage: Message = { role: "user", text: userText };
        setMessages((prev) => [...prev, newMessage]);
        setLoading(true);

        // ✅ null-safe 숫자 변환 함수
        const safe = (val: any, digits: number = 1) => {
            if (val === null || val === undefined || isNaN(val)) return (0).toFixed(digits);
            return Number(val).toFixed(digits);
        };

        try {
            const res = await api.post<DailyAnalysis>("/ai/meals", { text: userText });
            const data = res.data;

            // ✅ AI의 액션 피드백
            const actionText =
                data.action === "update"
                    ? "✏️ 기존 식단이 수정되었어요!"
                    : data.action === "delete"
                        ? "🗑️ 일부 식단이 삭제되었어요!"
                        : "🍽️ 새로운 식단이 추가되었어요!";

            // ✅ 총합 요약
            const summary = `🍱 오늘의 식단 분석 결과\n\n총 섭취 칼로리: ${safe(
                data.totalCalories,
                0
            )} kcal\n단백질: ${safe(data.totalProtein)} g\n지방: ${safe(
                data.totalFat
            )} g\n탄수화물: ${safe(data.totalCarbs)} g`;

            // ✅ 상세 식단 목록
            const mealDetails = (data.meals ?? [])
                .map((meal) => {
                    const mealNameMap: Record<string, string> = {
                        breakfast: "🥣 아침",
                        아침: "🥣 아침",
                        lunch: "🍛 점심",
                        점심: "🍛 점심",
                        dinner: "🍽️ 저녁",
                        저녁: "🍽️ 저녁",
                        snack: "🍪 간식",
                        간식: "🍪 간식",
                    };
                    const mealName = mealNameMap[meal.time] || "🍪 간식";

                    const foodLines = (meal.foods ?? [])
                        .map(
                            (f) =>
                                `- ${f.name} (${safe(f.quantity, 0)}${f.unit}) → ${safe(
                                    f.calories,
                                    0
                                )} kcal, P:${safe(f.protein)}g F:${safe(f.fat)}g C:${safe(f.carbs)}g`
                        )
                        .join("\n");

                    return `${mealName}\n${foodLines}`;
                })
                .join("\n\n");

            // ✅ 응답 메시지 구성
            const replyText = `${actionText}\n\n${summary}\n\n${mealDetails}`;

            setMessages((prev) => [...prev, { role: "ai", text: replyText }]);
        } catch (err) {
            console.error(err);
            setMessages((prev) => [
                ...prev,
                { role: "ai", text: "❌ 분석 중 오류가 발생했어요." },
            ]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col w-full max-w-lg mx-auto bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-4 h-[600px]">
            <div className="flex-1 overflow-y-auto space-y-3 mb-3">
                {messages.map((msg, i) => (
                    <ChatMessage key={i} role={msg.role} text={msg.text} />
                ))}
                {loading && <p className="text-sm text-gray-500">분석 중...</p>}
            </div>
            <ChatInput onSend={handleSend} disabled={loading} />
        </div>
    );
}
