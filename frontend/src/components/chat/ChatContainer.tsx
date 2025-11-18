import {useState, useEffect, useRef} from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import api from "../../api/axios";
import { useDashboard } from "../../context/DashboardContext";
import DeleteConfirmModal from "../DeleteConfirmModal.tsx";
import ManualModal from "./ManualModal.tsx";


// ======================= 타입 =======================
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
    action?: "add" | "update" | "delete" | "replace" | "error" | "none";
    meals: Meal[];
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
    deleteTargets?: string[];
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
    action: "add" | "update" | "delete" | "error" | "none";
    exercises: ExerciseItem[];
    totalCalories: number;
    totalDuration: number;
    deleteTargets?: string[];
}

interface EmotionResult {
    action?: "add" | "update" | "delete" | "error" | "none";
    primaryEmotion?: string | null;
    primaryScore?: number;
    summaries?: string[];
    keywords?: string[][];
    deleteTargets?: string[];
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

// ======================= 유틸 =======================
const safe = (v: any, d = 1) =>
    v === null || v === undefined || isNaN(v) ? (0).toFixed(d) : Number(v).toFixed(d);

// ------------------------------------------------------------
// 🔥 삭제 명령 감지 (프론트단에서도 동일하게 적용)
// ------------------------------------------------------------
const deleteAllPhrases = [
    "오늘 기록 전체 삭제",
    "전체기록삭제",
    "오늘 기록 다 삭제",
    "오늘 기록 전부 삭제",
    "오늘 전체 삭제",
    "기록 전체 삭제",
    "기록 전부 삭제",
    "전체 기록 삭제",
    "전부 다 지워",
    "모든 기록 삭제",
    "전체 초기화",
    "기록 초기화",
    "기록 다 지워",
    "기록 싹 지워",
    "오늘 기록 초기화",
    "다 지워줘",
    "전부 삭제해줘",
];

const deleteMealPhrases = [
    "식단 삭제",
    "식단삭제",
    "식단 초기화",
    "식단 전부 삭제",
    "식단 다 지워",
    "오늘 식단 삭제",
    "먹은거 삭제",
    "먹은 거 다 지워",
    "오늘 식단 초기화",
    "오늘 먹은거 리셋",
];

const deleteExercisePhrases = [
    "운동 삭제",
    "운동삭제",
    "운동 기록 삭제",
    "운동 초기화",
    "운동 다 지워",
    "오늘 운동 삭제",
    "운동 전부 삭제",
    "운동 리셋",
    "오늘 운동 다 지워",
];

const deleteEmotionPhrases = [
    "감정 삭제",
    "감정 기록 삭제",
    "감정 초기화",
    "기분 기록 삭제",
    "감정 다 지워",
    "오늘 감정 삭제",
    "감정 전부 삭제",
];

const includesAny = (text: string, arr: string[]) =>
    arr.some((p) => text.toLowerCase().includes(p));

const isDeleteAllRequest = (t: string) => includesAny(t, deleteAllPhrases);
const isDeleteMealRequest = (t: string) => includesAny(t, deleteMealPhrases);
const isDeleteExerciseRequest = (t: string) => includesAny(t, deleteExercisePhrases);
const isDeleteEmotionRequest = (t: string) => includesAny(t, deleteEmotionPhrases);

// ======================================================
// 🔥 ChatContainer 본문
// ======================================================
export default function ChatContainer() {
    const [messages, setMessages] = useState<Message[]>([]);
    const [loading, setLoading] = useState(false);
    const [isLoaded, setIsLoaded] = useState(false);
    const bottomRef = useRef<HTMLDivElement | null>(null);
    const { setShouldRefresh } = useDashboard();
    const [isManualOpen, setIsManualOpen] = useState(false);

    const [pendingDeleteType, setPendingDeleteType] = useState<
        null | "all" | "meal" | "exercise" | "emotion"
    >(null);
    const [pendingText, setPendingText] = useState<string>("");

    // 🔥 목표 기반 메시지
    const [userGoals, setUserGoals] = useState<string[]>([]);
    const [customGoal, setCustomGoal] = useState<string>("");
    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);
    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/user/profile");
                if (res.data.goalsDetailJson) {
                    setUserGoals(JSON.parse(res.data.goalsDetailJson).map((g: any) => g.goal));
                }
                if (res.data.goalText) setCustomGoal(res.data.goalText);
            } catch {}
        })();
    }, []);

    const buildGoalMessage = () => {
        if (userGoals.includes("체중 감량"))
            return "지금처럼만 유지한다면 체중 감량 목표에 점점 가까워지고 있어요! 🔥";
        if (userGoals.includes("체중 증가"))
            return "칼로리와 단백질을 조금만 더 보충하면 체중 증가에 도움이 돼요! 💪";
        if (userGoals.includes("근육량 증가"))
            return "운동 루틴과 식단이 근성장에 잘 맞아요! 멋진 흐름이에요. 🏋️‍♂️";
        if (userGoals.includes("체력 향상"))
            return "꾸준함이 최고의 체력! 오늘도 좋은 흐름을 이어가고 있어요. ⚡";
        if (userGoals.includes("스트레스 관리"))
            return "감정에 귀 기울이는 것 자체가 스트레스 관리에 큰 도움이 돼요. 🌿";
        if (customGoal)
            return `오늘 하루는 입력한 목표(“${customGoal}”)에 도움이 되는 방향으로 잘 보내셨어요!`;
        return "";
    };

    const [isChatDeleteModalOpen, setIsChatDeleteModalOpen] = useState(false);
    const cancelChatDelete = () => setIsChatDeleteModalOpen(false);
    const getTodayKey = () => {
        const today = new Date().toISOString().slice(0, 10);
        return `chatLogs_${today}`;
    };
    const handleClearChat = () => {
        setIsChatDeleteModalOpen(true);
    };
    const confirmChatDelete = () => {
        const key = getTodayKey();

        localStorage.removeItem(key);

        setMessages([
            {
                role: "ai",
                text:
                    "기록을 깔끔하게 정리해두었어요!\n" +
                    "오늘도 건강한 하루를 함께 만들어봐요."
            }
        ]);

        setIsChatDeleteModalOpen(false);
    };


    // ==================== 로컬스토리지 ====================
    const todayKey = () => "chatLogs_" + new Date().toISOString().slice(0, 10);

    useEffect(() => {
        const saved = localStorage.getItem(todayKey());
        if (saved) {
            try {
                setMessages(JSON.parse(saved));
            } catch {
                setMessages([]);
            }
        } else {
            setMessages([
                { role: "ai", text: "안녕하세요 👋 오늘의 식단·운동·감정을 함께 기록해볼까요?" },
            ]);
        }

        Object.keys(localStorage).forEach((k) => {
            if (k.startsWith("chatLogs_") && k !== todayKey()) localStorage.removeItem(k);
        });

        setIsLoaded(true);
    }, []);

    useEffect(() => {
        if (!isLoaded) return;
        localStorage.setItem(todayKey(), JSON.stringify(messages));
    }, [messages, isLoaded]);

    // ======================================================
    // 🔥 실제 분석 처리 함수
    // ======================================================
    const processUserMessage = async (userText: string) => {
        try {
            const res = await api.post<UnifiedAnalysisResult>("/ai/analyze", { text: userText });
            const data = res.data;

            let reply = "";

            // ----------------- 식단 -----------------
            if (data.mealAnalysis) {
                const m = data.mealAnalysis;

                if (m.action === "delete") {
                    const targets = m.deleteTargets ?? [];
                    if (targets.length > 0)
                        reply += `🍱 ${targets.join(", ")} 식단을 삭제했어요!\n\n`;
                    else
                        reply += "🍱 오늘 식단 기록을 모두 삭제했어요!\n\n";
                }
                else if (m.action !== "none" && m.meals.length > 0) {
                    reply += `🍱 [식단 요약]\n총 섭취 ${safe(m.totalCalories,0)} kcal\n\n`;
                    reply += m.meals
                        .map(meal => {
                            const foods = meal.foods
                                .map(f => `- ${f.name} ${safe(f.calories,0)} kcal`)
                                .join("\n");
                            return `${meal.time}\n${foods}`;
                        })
                        .join("\n\n");
                    reply += "\n\n";
                }
            }

            // ----------------- 운동 -----------------
            if (data.exerciseAnalysis) {
                const ex = data.exerciseAnalysis;
                if (ex.action === "delete") {
                    const targets = ex.deleteTargets ?? [];
                    if (targets.length > 0)
                        reply += `💪 ${targets.join(", ")} 운동을 삭제했어요!\n\n`;
                    else
                        reply += "💪 오늘 운동 기록을 모두 삭제했어요!\n\n";
                }
                else if (ex.action !== "none" && ex.exercises.length > 0) {
                    reply += `💪 [운동 요약]\n총 ${safe(ex.totalDuration,0)}분 소모 ${safe(ex.totalCalories,0)} kcal\n\n`;
                    reply += ex.exercises
                        .map(e => `- ${e.name} ${e.durationMin}분 → ${e.calories} kcal`)
                        .join("\n");
                    reply += "\n\n";
                }
            }

            // ----------------- 감정 -----------------
            if (data.emotionAnalysis) {
                const emo = data.emotionAnalysis;
                if (emo.action === "delete") {
                    const targets = emo.deleteTargets ?? [];
                    if (targets.length > 0)
                        reply += `💬 ${targets.join(", ")} 감정을 삭제했어요!\n\n`;
                    else
                        reply += "💬 오늘의 감정 기록을 모두 삭제했어요!\n\n";
                }
                else if (emo.primaryEmotion) {
                    reply += `💬 [감정 분석] ${emo.primaryEmotion} (${safe(emo.primaryScore,0)}점)\n\n`;
                }
            }

            const goalMsg = buildGoalMessage();
            if (goalMsg) reply += `${goalMsg}\n`;

            setMessages(p => [...p, { role: "ai", text: reply || "분석할 내용이 없어요!" }]);
            setShouldRefresh(true);

        } catch {
            setMessages(p => [...p, { role: "ai", text: "❌ 분석 중 문제가 발생했어요." }]);
        }
    };

    // ======================================================
    // 🔥 메시지 전송 핸들러
    // ======================================================
    const handleSend = (userText: string) => {
        if (!userText.trim()) return;

        setMessages((prev) => [...prev, { role: "user", text: userText }]);

        // 1) 삭제 요청 감지 → 모달 띄우기
        if (isDeleteAllRequest(userText)) {
            setPendingDeleteType("all");
            setPendingText(userText);
            return;
        }
        if (isDeleteMealRequest(userText)) {
            setPendingDeleteType("meal");
            setPendingText(userText);
            return;
        }
        if (isDeleteExerciseRequest(userText)) {
            setPendingDeleteType("exercise");
            setPendingText(userText);
            return;
        }
        if (isDeleteEmotionRequest(userText)) {
            setPendingDeleteType("emotion");
            setPendingText(userText);
            return;
        }

        // 2) 일반 메시지 → 바로 처리
        setLoading(true);
        processUserMessage(userText).finally(() => setLoading(false));
    };

    // ======================================================
    // 🔥 삭제 모달 핸들러
    // ======================================================
    const handleConfirmDelete = () => {
        const text = pendingText;
        setPendingDeleteType(null);
        setPendingText("");

        setLoading(true);
        processUserMessage(text).finally(() => setLoading(false));
    };

    const handleCancelDelete = () => {
        setMessages((prev) => [...prev, { role: "ai", text: "삭제를 취소했어요!" }]);
        setPendingDeleteType(null);
        setPendingText("");
    };

    if (!isLoaded) return null;

    // 삭제 종류별 안내 메시지
    const deleteMessages = {
        all: "오늘의 모든 건강 기록(식단, 운동, 감정 포함)이 삭제돼요.",
        meal: "오늘의 식단 기록이 삭제돼요.",
        exercise: "오늘의 운동 기록이 삭제돼요.",
        emotion: "오늘의 감정 기록이 삭제돼요.",
    };

    return (
        <div className="flex flex-col w-full h-full bg-white dark:bg-gray-800">
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
                {messages.map((m, i) => (
                    <ChatMessage key={i} role={m.role} text={m.text} />
                ))}

                {loading && <p className="text-sm text-gray-500">AI가 분석 중입니다...</p>}
                <div ref={bottomRef} />
            </div>

            <div className="border-t border-gray-300 dark:border-gray-700 px-3 py-3 flex-shrink-0">
                <ChatInput
                    onSend={handleSend}
                    disabled={loading}
                    onOpenManual={() => setIsManualOpen(true)}
                    onClearChat={handleClearChat}
                />
            </div>
            <ManualModal open={isManualOpen} onClose={() => setIsManualOpen(false)} />
            <DeleteConfirmModal
                open={pendingDeleteType !== null}
                message={pendingDeleteType ? deleteMessages[pendingDeleteType] : ""}
                onCancel={handleCancelDelete}
                onConfirm={handleConfirmDelete}
            />
            <DeleteConfirmModal
                open={isChatDeleteModalOpen}
                message="오늘의 채팅 기록을 모두 삭제할까요?"
                onCancel={cancelChatDelete}
                onConfirm={confirmChatDelete}
            />

        </div>

    );
}
