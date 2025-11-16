import type {Variants} from "framer-motion";
import {AnimatePresence, motion} from "framer-motion";
import GoalStepDetail from "./GoalStepDetail.tsx";
import {LuTarget} from "react-icons/lu";

type Props = {
    step: "main" | "detail";
    setStep: (step: "main" | "detail") => void;
    selectedGoals: string[];
    setSelectedGoals: React.Dispatch<React.SetStateAction<string[]>>;
    toggleGoal: (goal: string) => void;
    customGoal: string;
    setCustomGoal: (v: string) => void;
    handleNext: () => void;
    onClose: () => void;
    onSave: (details: any[], text: string) => void;
    existingDetails?: { goal: string; factors: string[] }[];
};

const GOAL_OPTIONS = [
    "체중 감량",
    "체중 유지",
    "체중 증가",
    "근육량 증가",
    "식단 관리",
    "스트레스 관리",
    "기타 (직접 입력)",
];

const EASE_OUT: [number, number, number, number] = [0.16, 1, 0.3, 1];
const EASE_IN: [number, number, number, number] = [0.4, 0, 1, 1];

export const backdrop: Variants = {
    initial: { opacity: 0 },
    animate: { opacity: 1, transition: { duration: 0.25, ease: EASE_OUT } },
    exit: { opacity: 0, transition: { duration: 0.2, ease: EASE_IN } },
};

export const modal: Variants = {
    initial: { opacity: 0, y: 25, scale: 0.96 },
    animate: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.3, ease: EASE_OUT } },
    exit: { opacity: 0, y: 25, scale: 0.96, transition: { duration: 0.25, ease: EASE_IN } },
};

/** ✅ GoalModal Component */
export default function GoalModal({
                                      step,
                                      setStep,
                                      selectedGoals,
                                      toggleGoal,
                                      customGoal,
                                      setCustomGoal,
                                      handleNext,
                                      onClose,
                                      onSave,
                                      existingDetails,
                                      setSelectedGoals,
                                  }: Props) {

    /** ✅ “기타” 입력 시 ProfilePage로 직접 전달 */
    const handleCustomGoalSave = (text: string) => {
        const trimmed = text.trim();
        if (!trimmed) return;

        const details = [{ goal: "기타 (직접 입력)", factors: [trimmed] }];
        onSave(details, trimmed);
        onClose(); // 모달 닫기
    };


    return (
        <AnimatePresence mode="wait">
            <motion.div
                className="fixed inset-0 z-50 flex items-center justify-center"
                variants={backdrop}
                initial="initial"
                animate="animate"
                exit="exit"
            >
                <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

                <motion.div
                    variants={modal}
                    initial="initial"
                    animate="animate"
                    exit="exit"
                    className="relative bg-white dark:bg-gray-900 rounded-2xl shadow-2xl p-10 w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700"
                >
                    <AnimatePresence mode="wait" initial={false}>
                        {step === "main" ? (
                            <motion.div
                                key="step-main"
                                initial={{ opacity: 0, x: 40 }}
                                animate={{
                                    opacity: 1,
                                    x: 0,
                                    transition: { duration: 0.28, ease: [0.16, 1, 0.3, 1] },
                                }}
                                exit={{
                                    opacity: 0,
                                    x: -40,
                                    transition: { duration: 0.22, ease: [0.4, 0, 1, 1] },
                                }}
                            >
                                <h3 className="text-2xl font-semibold mb-6 text-gray-900 dark:text-gray-100 flex items-center gap-2">
                                    <LuTarget className="w-6 h-6 text-pink-500" />
                                    목표 설정
                                </h3>
                                <p className="text-sm text-gray-600 dark:text-gray-400 mb-6 leading-relaxed">
                                    최대 <b>3가지</b>까지 선택할 수 있어요.<br />
                                    <span className="text-blue-600 dark:text-blue-400">
                                        체중 관련(감량·유지·증가)
                                    </span>
                                    은 한 가지만 선택 가능합니다.
                                </p>

                                {/* ✅ 기본 목표 목록 */}
                                <div className="space-y-3 mb-4">
                                    {GOAL_OPTIONS.map((goal) => (
                                        <motion.button
                                            key={goal}
                                            whileHover={{ scale: 1.02 }}
                                            whileTap={{ scale: 0.98 }}
                                            type="button"
                                            onClick={() => {
                                                const isCustom = goal === "기타 (직접 입력)";
                                                const hasCustomSelected = selectedGoals.includes("기타 (직접 입력)");

                                                if (isCustom) {
                                                    if (hasCustomSelected) {
                                                        toggleGoal("기타 (직접 입력)");
                                                    } else {
                                                        setSelectedGoals(["기타 (직접 입력)"]);
                                                    }
                                                    return;
                                                }

                                                if (hasCustomSelected) {
                                                    setSelectedGoals([]); // 기타 해제 후 새 목표 선택
                                                }

                                                toggleGoal(goal);
                                            }}
                                            className={`w-full px-4 py-3 border rounded-md text-left transition ${
                                                selectedGoals.includes(goal)
                                                    ? "bg-blue-600 text-white border-blue-600 shadow-md"
                                                    : "bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-300 border-gray-400 dark:border-gray-600 hover:bg-gray-200 dark:hover:bg-gray-700"
                                            }`}
                                        >
                                            {goal}
                                        </motion.button>
                                    ))}
                                </div>

                                {/* ✅ 기타 선택 시 자유 입력 + 즉시 저장 버튼 */}
                                <AnimatePresence>
                                    {selectedGoals.includes("기타 (직접 입력)") && (
                                        <motion.div
                                            key="custom-goal-section"
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            exit={{ opacity: 0, y: -10 }}
                                            transition={{ duration: 0.25 }}
                                            className="mt-4"
                                        >
                                            <textarea
                                                value={customGoal}
                                                onChange={(e) => setCustomGoal(e.target.value)}
                                                placeholder="자신의 목표를 자유롭게 입력하세요."
                                                className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-3 h-24 resize-none bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none mb-4"
                                            />
                                            <div className="flex justify-end gap-3">
                                                <motion.button
                                                    whileTap={{ scale: 0.97 }}
                                                    onClick={onClose}
                                                    className="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition"
                                                >
                                                    취소
                                                </motion.button>
                                                <motion.button
                                                    whileTap={{ scale: 0.97 }}
                                                    disabled={!customGoal.trim()}
                                                    onClick={() => handleCustomGoalSave(customGoal)}
                                                    className={`px-5 py-2 rounded-md transition ${
                                                        !customGoal.trim()
                                                            ? "bg-gray-400 text-white cursor-not-allowed"
                                                            : "bg-blue-600 text-white hover:bg-blue-700"
                                                    }`}
                                                >
                                                    저장하기
                                                </motion.button>
                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>

                                {!selectedGoals.includes("기타 (직접 입력)") && (
                                    <div className="flex justify-end gap-3 mt-6">
                                        <motion.button
                                            whileTap={{ scale: 0.97 }}
                                            onClick={onClose}
                                            className="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition"
                                        >
                                            취소
                                        </motion.button>
                                        <motion.button
                                            whileTap={{ scale: selectedGoals.length > 0 ? 0.97 : 1 }}
                                            disabled={selectedGoals.length === 0}
                                            onClick={handleNext}
                                            className={`px-5 py-2 rounded-md transition font-medium ${
                                                selectedGoals.length === 0
                                                    ? "bg-gray-400 text-white cursor-not-allowed"
                                                    : "bg-blue-600 text-white hover:bg-blue-700"
                                            }`}
                                        >
                                            다음
                                        </motion.button>
                                    </div>
                                )}
                            </motion.div>
                        ) : (
                            <motion.div
                                key="step-detail"
                                initial={{ opacity: 0, x: 40 }}
                                animate={{
                                    opacity: 1,
                                    x: 0,
                                    transition: { duration: 0.28, ease: [0.16, 1, 0.3, 1] },
                                }}
                                exit={{
                                    opacity: 0,
                                    x: -40,
                                    transition: { duration: 0.22, ease: [0.4, 0, 1, 1] },
                                }}
                            >
                                <GoalStepDetail
                                    goals={selectedGoals}
                                    existingDetails={existingDetails}
                                    onBack={() => setStep("main")}
                                    onClose={onClose}
                                    onSave={(updatedDetails) => {
                                        const filtered = updatedDetails.filter((d) =>
                                            selectedGoals.includes(d.goal)
                                        );
                                        onSave(filtered, customGoal);
                                    }}
                                />
                            </motion.div>
                        )}
                    </AnimatePresence>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}
