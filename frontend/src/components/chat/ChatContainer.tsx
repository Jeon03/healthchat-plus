import { useState } from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import api from "../../api/axios";
import { useDashboard } from "../../context/DashboardContext";

type ChatRole = "user" | "ai";

/* ---------- 타입 정의들 ---------- */
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
    action?: "add" | "update" | "delete" | "replace" | "error";
    targetMeal?: string;
    meals: Meal[];
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
}

interface ExerciseItem {
    category: string;
    part: string;
    name: string;
    durationMin: number;
    intensity: string;
    calories: number;
}

interface ExerciseAnalysisResult {
    action: string;
    exercises: ExerciseItem[];
    totalCalories: number;
    totalDuration: number;
    message?: string;
}

interface EmotionResult {
    primaryEmotion: string;
    primaryScore: number;
    summaries: string[];
    keywords: string[];
}

interface UnifiedAnalysisResult {
    mealAnalysis?: DailyAnalysis;
    exerciseAnalysis?: ExerciseAnalysisResult;
    emotionAnalysis?: EmotionResult;
}

interface Message {
    role: ChatRole;
    text: string;
}

export default function ChatContainer() {
    const [messages, setMessages] = useState<Message[]>([
        { role: "ai", text: "안녕하세요 👋 오늘의 식단, 운동, 감정을 함께 기록해볼까요?" },
    ]);
    const [loading, setLoading] = useState(false);
    const { setShouldRefresh } = useDashboard();

    const safe = (val: any, digits: number = 1) => {
        if (val === null || val === undefined || isNaN(val)) return (0).toFixed(digits);
        return Number(val).toFixed(digits);
    };

    /** -----------------------------------------
     *     🔥 메인 메시지 전송 + 통합 분석 처리
     --------------------------------------------*/
    const handleSend = async (userText: string) => {
        if (!userText.trim()) return;

        setMessages((prev) => [...prev, { role: "user", text: userText }]);
        setLoading(true);

        try {
            const res = await api.post<UnifiedAnalysisResult>("/ai/analyze", { text: userText });
            const data = res.data;

            let replyText = "";

            /* ------------------- 🍱 식단 ------------------- */
            if (data.mealAnalysis) {
                const meal = data.mealAnalysis;

                if (meal.action === "error") {
                    replyText += "🍱 [식단 분석 실패]\n식단 정보를 이해하지 못했어요. 다시 입력해 주세요! 🙏\n\n";
                } else {
                    replyText += `🍱 [식단 요약]\n총 섭취 칼로리: ${safe(meal.totalCalories, 0)} kcal\n`;
                    replyText += `단백질: ${safe(meal.totalProtein)}g, 지방: ${safe(
                        meal.totalFat
                    )}g, 탄수화물: ${safe(meal.totalCarbs)}g\n\n`;

                    if (meal.meals?.length) {
                        replyText += meal.meals
                            .map((m) => {
                                const foods = m.foods
                                    .map(
                                        (f) =>
                                            `- ${f.name} (${safe(f.quantity, 0)}${
                                                f.unit
                                            }) → ${safe(f.calories, 0)} kcal`
                                    )
                                    .join("\n");

                                return `${m.time}\n${foods}`;
                            })
                            .join("\n\n");
                        replyText += "\n\n";
                    }
                }
            }

            /* ------------------- 💪 운동 ------------------- */
            if (data.exerciseAnalysis) {
                const ex = data.exerciseAnalysis;

                if (ex.action === "error") {
                    replyText += "💪 [운동 분석 실패]\n운동 내용을 이해하지 못했어요. 조금 더 자세히 적어볼까요? 😊\n\n";
                } else {
                    replyText += `💪 [운동 요약]\n`;
                    replyText += `총 운동 시간: ${safe(ex.totalDuration, 0)}분\n총 소모 칼로리: ${safe(
                        ex.totalCalories,
                        0
                    )} kcal\n\n`;

                    if (ex.exercises?.length > 0) {
                        replyText += ex.exercises
                            .map(
                                (e) =>
                                    `- ${e.name} (${e.category}/${e.part}) ${e.durationMin}분 (${e.intensity}) → ${e.calories} kcal`
                            )
                            .join("\n");
                        replyText += "\n\n";
                    }
                }
            }

            /* ------------------- 💬 감정 ------------------- */
            if (data.emotionAnalysis) {
                const emo = data.emotionAnalysis;

                replyText += `💬 [감정 분석]\n`;
                replyText += `대표 감정: ${emo.primaryEmotion} (${safe(emo.primaryScore, 0)}점)\n\n`;

                if (emo.summaries?.length > 0) {
                    replyText += `📝 감정 흐름 요약:\n`;
                    replyText += emo.summaries.map((s) => `- ${s}`).join("\n");
                    replyText += "\n\n";
                }

                if (emo.keywords?.length > 0) {
                    replyText += `🔖 주요 키워드: ${emo.keywords.join(", ")}\n\n`;
                }
            }

            /* 결과 메시지 삽입 */
            setMessages((prev) => [
                ...prev,
                { role: "ai", text: replyText || "분석 결과가 없어요." },
            ]);

            // 🔄 대시보드 새로고침
            setShouldRefresh(true);

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
                {loading && <p className="text-sm text-gray-500">AI가 분석 중입니다...</p>}
            </div>
            <ChatInput onSend={handleSend} disabled={loading} />
        </div>
    );
}
