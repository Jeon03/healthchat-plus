import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";

type Props = {
    goals: string[];
    onBack: () => void;
    onClose: () => void;
    onSave: (details: any[]) => void;
    existingDetails?: { goal: string; factors: string[] }[]; // ✅ 기존 선택 상태 전달
};

export default function GoalStepDetail({ goals, onBack, onClose, onSave, existingDetails }: Props) {
    const [index, setIndex] = useState(0);

    // ✅ 기존 선택값이 있다면 answers 초기화 시점에 반영
    const [answers, setAnswers] = useState<Record<string, string[]>>(() => {
        if (existingDetails && existingDetails.length > 0) {
            return existingDetails.reduce((acc, cur) => {
                acc[cur.goal] = cur.factors;
                return acc;
            }, {} as Record<string, string[]>);
        }
        return {};
    });

    const currentGoal = goals[index];
    const options = getOptionsByGoal(currentGoal);
    const selected = answers[currentGoal] || [];

    const toggleOption = (opt: string) => {
        if (selected.includes(opt)) {
            setAnswers((prev) => ({
                ...prev,
                [currentGoal]: prev[currentGoal].filter((o) => o !== opt),
            }));
        } else {
            setAnswers((prev) => ({
                ...prev,
                [currentGoal]: [...(prev[currentGoal] || []), opt],
            }));
        }
    };

    const handleNext = () => {
        if (index < goals.length - 1) {
            setIndex(index + 1);
        } else {
            // ✅ 모든 목표 응답 완료 시 상위로 전달
            const details = Object.entries(answers).map(([goal, factors]) => ({
                goal,
                factors,
            }));

            console.log("🎯 모든 목표 응답:", details);
            onSave(details);
            onClose();
        }
    };

    // ✅ 선택된 목표 변경 시 기존 응답 자동 반영
    useEffect(() => {
        if (existingDetails && existingDetails.length > 0) {
            const existing = existingDetails.find((d) => d.goal === currentGoal);
            if (existing && existing.factors) {
                setAnswers((prev) => ({
                    ...prev,
                    [currentGoal]: existing.factors,
                }));
            }
        }
    }, [currentGoal]);

    return (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50">
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.98 }}
                animate={{
                    opacity: 1,
                    y: 0,
                    scale: 1,
                    transition: { duration: 0.25, ease: [0.16, 1, 0.3, 1] },
                }}
                exit={{
                    opacity: 0,
                    y: 20,
                    scale: 0.98,
                    transition: { duration: 0.18, ease: [0.4, 0, 1, 1] },
                }}
                className="bg-white dark:bg-gray-900 rounded-xl shadow-2xl p-8 w-full max-w-2xl max-h-[90vh] overflow-y-auto"
            >
                <AnimatePresence mode="wait">
                    <motion.div
                        key={index}
                        initial={{ opacity: 0, x: 40 }}
                        animate={{
                            opacity: 1,
                            x: 0,
                            transition: { duration: 0.3, ease: [0.16, 1, 0.3, 1] },
                        }}
                        exit={{
                            opacity: 0,
                            x: -40,
                            transition: { duration: 0.25, ease: [0.4, 0, 1, 1] },
                        }}
                    >
                        {/* 제목 */}
                        <motion.h3
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0, transition: { duration: 0.25 } }}
                            className="text-xl font-semibold mb-2 text-gray-900 dark:text-white"
                        >
                            {getQuestionByGoal(currentGoal)}
                        </motion.h3>

                        {/* 설명 */}
                        <motion.p
                            initial={{ opacity: 0, y: 10 }}
                            animate={{
                                opacity: 1,
                                y: 0,
                                transition: { duration: 0.3, delay: 0.1 },
                            }}
                            className="text-sm text-gray-500 mb-6"
                        >
                            해당하는 항목을 모두 선택해주세요.
                        </motion.p>

                        {/* 옵션 리스트 */}
                        <motion.div
                            className="space-y-3 mb-8"
                            initial="hidden"
                            animate="visible"
                            variants={{
                                hidden: { opacity: 1 },
                                visible: {
                                    opacity: 1,
                                    transition: {
                                        staggerChildren: 0.05,
                                        delayChildren: 0.05,
                                    },
                                },
                            }}
                        >
                            {options.map((opt) => (
                                <motion.button
                                    key={opt}
                                    variants={{
                                        hidden: { opacity: 0, y: 8 },
                                        visible: {
                                            opacity: 1,
                                            y: 0,
                                            transition: { duration: 0.25 },
                                        },
                                    }}
                                    whileHover={{ scale: 1.02 }}
                                    whileTap={{ scale: 0.98 }}
                                    onClick={() => toggleOption(opt)}
                                    className={`w-full px-4 py-3 border rounded-md text-left transition-all ${
                                        selected.includes(opt)
                                            ? "bg-blue-600 text-white border-blue-600 shadow-md"
                                            : "bg-gray-100 dark:bg-gray-800 border-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700"
                                    }`}
                                >
                                    {opt}
                                </motion.button>
                            ))}
                        </motion.div>

                        {/* 버튼 */}
                        <motion.div
                            className="flex justify-between"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{
                                opacity: 1,
                                y: 0,
                                transition: { delay: 0.2, duration: 0.3 },
                            }}
                        >
                            <motion.button
                                whileTap={{ scale: 0.97 }}
                                onClick={() => (index === 0 ? onBack() : setIndex(index - 1))}
                                className="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition"
                            >
                                ← 이전
                            </motion.button>
                            <motion.button
                                whileTap={{ scale: 0.97 }}
                                onClick={handleNext}
                                className="px-5 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition"
                            >
                                {index < goals.length - 1 ? "다음 →" : "완료 🎉"}
                            </motion.button>
                        </motion.div>
                    </motion.div>
                </AnimatePresence>
            </motion.div>
        </div>
    );
}

/** ✅ 질문 매핑 */
function getQuestionByGoal(goal: string): string {
    switch (goal) {
        case "체중 감량":
        case "체중 유지":
            return "체중 관리가 어려웠던 이유는 무엇인가요?";
        case "체중 증가":
            return "체중을 늘리고 싶은 이유가 무엇인가요?";
        case "근육량 증가":
            return "근육을 늘려서 어떤 목표를 이루고 싶으신가요?";
        case "식단 관리":
            return "식단 조절과 관련해 어떤 부분에 집중하고 싶으신가요?";
        case "스트레스 관리":
            return '다음 문장을 완성해주세요: "나는 ______ 할 때 기분이 좋아집니다."';
        default:
            return "당신의 목표를 조금 더 구체적으로 알려주세요.";
    }
}

/** ✅ 목표별 선택 옵션 */
function getOptionsByGoal(goal: string): string[] {
    switch (goal) {
        case "체중 감량":
        case "체중 유지":
            return [
                "시간이 부족해서",
                "식단을 꾸준히 지키기 어려워서",
                "음식이 입맛에 맞지 않아서",
                "무엇을 먹어야 할지 몰라서",
                "약속이나 외식이 많아서",
                "식욕을 참기 어려워서",
                "변화가 느리다고 느껴서",
            ];
        case "체중 증가":
            return [
                "운동 능력을 높이고 싶어서",
                "근육을 더 키우고 싶어서",
                "저체중이라서 건강이 걱정돼서",
                "의사나 트레이너의 권유로",
                "기타 (직접 입력)",
            ];
        case "근육량 증가":
            return [
                "눈에 보이는 탄탄한 몸을 만들고 싶어요",
                "전체적인 체형 밸런스를 키우고 싶어요",
                "힘을 더 키워서 무게를 늘리고 싶어요",
            ];
        case "식단 관리":
            return [
                "영양소(매크로)를 꾸준히 기록하고 싶어요",
                "완전 채식을 시도하고 싶어요",
                "부분 채식으로 조절하고 싶어요",
                "설탕 섭취를 줄이고 싶어요",
                "단백질을 더 늘리고 싶어요",
                "유제품을 줄이고 싶어요",
                "탄수화물 섭취를 줄이고 싶어요",
                "지방 섭취를 줄이고 싶어요",
                "과일과 채소를 더 먹고 싶어요",
                "기타 (직접 입력)",
            ];
        case "스트레스 관리":
            return [
                "산책하거나 걷기",
                "가볍게 달리기",
                "근력 운동하기",
                "자전거 타기",
                "요가나 필라테스 수업 듣기",
                "스트레칭",
                "명상이나 호흡 연습",
                "음악 듣기",
                "다른 활동하기",
                "아무것도 효과가 없었어요",
            ];
        default:
            return ["직접 입력"];
    }
}
