import { useState, useEffect } from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import api from "../../api/axios";
import { useDashboard } from "../../context/DashboardContext";

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
    /** 🔥 messages 초기값은 [] (null 금지) */
    const [messages, setMessages] = useState<Message[]>([]);
    const [isLoaded, setIsLoaded] = useState(false);
    const [loading, setLoading] = useState(false);
    const { setShouldRefresh } = useDashboard();

    /* 오늘 날짜 key */
    const getTodayKey = () => {
        const today = new Date().toISOString().slice(0, 10);
        return `chatLogs_${today}`;
    };

    /* ---------------------------------------------------
     *  📌 첫 진입 시 localStorage 불러오기
     * --------------------------------------------------- */
    useEffect(() => {
        const todayKey = getTodayKey();

        // 저장된 오늘 채팅 불러오기
        const saved = localStorage.getItem(todayKey);

        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                if (Array.isArray(parsed)) {
                    setMessages(parsed);
                } else {
                    setMessages([]);
                }
            } catch {
                setMessages([]);
            }
        } else {
            // 저장된 기록 없음 → 기본 메시지 제공
            setMessages([
                {
                    role: "ai",
                    text: "안녕하세요 👋 오늘의 식단, 운동, 감정을 함께 기록해볼까요?",
                },
            ]);
        }

        // 오래된 날짜 자동 삭제
        Object.keys(localStorage).forEach((key) => {
            if (key.startsWith("chatLogs_") && key !== todayKey) {
                localStorage.removeItem(key);
            }
        });

        setIsLoaded(true);
    }, []);

    /* ---------------------------------------------------
     *  💾 messages 변경되면 localStorage 저장
     * --------------------------------------------------- */
    useEffect(() => {
        if (!isLoaded) return; // 초기 로딩 중일 때는 저장 금지
        const todayKey = getTodayKey();
        localStorage.setItem(todayKey, JSON.stringify(messages));
    }, [messages, isLoaded]);

    const safe = (val: any, digits: number = 1) => {
        if (val === null || val === undefined || isNaN(val)) return (0).toFixed(digits);
        return Number(val).toFixed(digits);
    };

    /** ----------------------------------------------------------------------
     *   🔥 메시지 전송 + 통합 분석
     ------------------------------------------------------------------------ */
    const handleSend = async (userText: string) => {
        if (!userText.trim()) return;

        setMessages((prev) => [...prev, { role: "user", text: userText }]);
        setLoading(true);

        try {
            const res = await api.post<UnifiedAnalysisResult>("/ai/analyze", { text: userText });
            const data = res.data;

            let replyText = "";

            /* 🍱 식단 분석 */
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

            /* 💪 운동 분석 */
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

                    if (ex.exercises?.length) {
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

            /* 💬 감정 분석 */
            if (data.emotionAnalysis) {
                const emo = data.emotionAnalysis;

                replyText += `💬 [감정 분석]\n`;
                replyText += `대표 감정: ${emo.primaryEmotion} (${safe(emo.primaryScore, 0)}점)\n\n`;

                if (emo.summaries?.length) {
                    replyText += `📝 감정 흐름 요약:\n`;
                    replyText += emo.summaries.map((s) => `- ${s}`).join("\n");
                    replyText += "\n\n";
                }

                if (emo.keywords?.length) {
                    replyText += `🔖 주요 키워드: ${emo.keywords.join(", ")}\n\n`;
                }
            }

            /** 📌 아무 분석 결과도 없을 때 → 안내 메시지 제공 */
            if (!replyText.trim()) {
                replyText =
                    "입력하신 내용을 이해하기 어려웠어요 😅\n\n" +
                    "조금 더 구체적으로 적어주시면 분석해드릴게요!\n\n" +
                    "예시:\n" +
                    "• 아침에 샌드위치 먹었어\n" +
                    "• 저녁에 30분 조깅했어\n" +
                    "• 오늘 회사에서 스트레스 받았어\n\n" +
                    "식단·운동·감정 중 아무 내용이나 자유롭게 입력해주세요! 😊";
            }

            setMessages((prev) => [...prev, { role: "ai", text: replyText }]);
            setShouldRefresh(true);
        } catch {
            setMessages((prev) => [
                ...prev,
                { role: "ai", text: "❌ 분석 중 오류가 발생했어요." },
            ]);
        } finally {
            setLoading(false);
        }
    };

    if (!isLoaded) return null;

    return (
        <div className="flex flex-col w-full h-full bg-white dark:bg-gray-800">

            {/* 🔼 메시지 영역 — 스크롤됨 */}
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
                {messages.map((msg, i) => (
                    <ChatMessage key={i} role={msg.role} text={msg.text} />
                ))}
                {loading && <p className="text-sm text-gray-500">AI가 분석 중입니다...</p>}
            </div>

            {/* 🔽 입력창 — 아래 고정 */}
            <div className="border-t border-gray-300 dark:border-gray-700 px-3 py-3 flex-shrink-0">
                <ChatInput onSend={handleSend} disabled={loading} />
            </div>

        </div>
    );
}
