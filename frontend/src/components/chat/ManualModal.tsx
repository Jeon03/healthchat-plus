import { motion } from "framer-motion";
import {
    FiX,
    FiPlusCircle,
    FiEdit2,
    FiTrash,
    FiInfo,
    FiSmile,
    FiActivity,
} from "react-icons/fi";
import { PiBowlFoodBold } from "react-icons/pi";
import { useEffect } from "react";

interface ManualModalProps {
    open: boolean;
    onClose: () => void;
}

export default function ManualModal({ open, onClose }: ManualModalProps) {

    // ✅ 스크롤 잠금 훅 — 컴포넌트 내부로 이동
    useEffect(() => {
        if (open) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "";
        }
        return () => {
            document.body.style.overflow = "";
        };
    }, [open]);

    if (!open) return null;

    return (
        <div className="fixed inset-0 flex items-center justify-center z-50 pointer-events-none">
            <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="
                    bg-white dark:bg-gray-800 p-6 rounded-2xl w-[90%] max-w-2xl
                    shadow-xl relative max-h-[85vh] overflow-y-auto pointer-events-auto
                "
            >
                {/* 닫기 */}
                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 text-gray-500 hover:text-gray-800 dark:hover:text-gray-200"
                >
                    <FiX className="w-6 h-6" />
                </button>

                {/* 제목 */}
                <h2 className="text-2xl font-bold mb-4 text-gray-900 dark:text-gray-100 flex items-center gap-2">
                    <FiInfo className="text-blue-600" />
                    AI 코치 사용 설명서
                </h2>

                <p className="text-gray-600 dark:text-gray-300 mb-6 leading-relaxed">
                    자연어로 입력하면 AI가 식단·운동·감정을 자동 분석해 기록합니다.
                </p>

                {/* 🔹 하루 예시 */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-3">
                        <FiInfo /> 하루 일과 입력 예시
                    </h3>

                    <div className="text-sm bg-blue-50 dark:bg-blue-900/30 text-gray-700 dark:text-gray-200 p-4 rounded-xl border border-blue-200 dark:border-blue-700 leading-relaxed">
                        AI는 하루 일과를 자연스럽게 입력해도 식단·운동·감정을 자동으로 구분합니다.
                        <br /><br />
                        예시:<br />
                        • “아침에 빵 먹고 점심엔 김치찌개 먹었어. 오후엔 조깅 30분 하고 회사 일 때문에 스트레스 받았어.”<br /><br />
                        • “아침 굶고 점심에 비빔밥 먹었어. 운동은 팔굽혀펴기 20개 했고 기분은 괜찮았어.”
                    </div>
                </div>

                {/* 🔹 운동 */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-3">
                        <FiActivity /> 운동 기록 사용법
                    </h3>

                    {/* ADD */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 text-blue-600 dark:text-blue-400 font-semibold mb-1">
                            <FiPlusCircle /> 추가(Add)
                        </div>
                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            새로운 운동 추가 또는 기존 운동 시간 누적이 가능합니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg leading-relaxed">
                            예시:<br />
                            • “러닝 30분 했어”<br />
                            • “조깅 1시간 더 했어”
                        </div>
                    </div>

                    {/* UPDATE */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400 font-semibold mb-1">
                            <FiEdit2 /> 수정(Update)
                        </div>
                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            기존 운동 기록을 수정하거나 교체할 수 있습니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg mb-3">
                            예시:<br />
                            • “러닝 시간을 20분으로 수정해줘”
                        </div>

                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            <span className="font-semibold">“A 말고 B 했어”</span>라고 말하면 자동으로 교체됩니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg">
                            예시:<br />
                            • “러닝 말고 팔굽혀펴기 20회 했어”<br />
                            → 러닝 삭제 + 팔굽혀펴기 추가
                        </div>
                    </div>
                </div>

                {/* 🔹 식단 */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-3">
                        <PiBowlFoodBold /> 식단 기록 사용법
                    </h3>

                    {/* ADD */}
                    <div className="mb-4">
                        <div className="flex items-center gap-2 text-blue-600 dark:text-blue-400 font-semibold mb-1">
                            <FiPlusCircle /> 추가(Add)
                        </div>
                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            아침·점심·저녁·간식을 자동 인식해 해당 식사에 기록합니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg mb-3 leading-relaxed">
                            예시:<br />
                            • “아침에 샌드위치 먹었어”<br />
                            • “점심으로 라면 먹었어”<br />
                            • “간식으로 초콜릿 먹었어”
                        </div>
                    </div>

                    {/* UPDATE */}
                    <div className="mb-4">
                        <div className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400 font-semibold mb-1">
                            <FiEdit2 /> 수정(Update)
                        </div>
                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            기존 식단을 수정하거나 다른 음식으로 교체할 수 있습니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg leading-relaxed">
                            예시:<br />
                            • “아침에 샌드위치 말고 김밥 먹었어”<br />
                            • “점심을 치킨으로 바꿔줘”
                        </div>
                    </div>

                    <p className="text-gray-700 dark:text-gray-300 mb-2">
                        음식 이름만 입력해도 AI가 자동으로 칼로리를 계산합니다.
                    </p>
                </div>

                {/* 🔹 감정 */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-3">
                        <FiSmile /> 감정 기록 사용법
                    </h3>

                    {/* ADD */}
                    <div className="mb-4">
                        <div className="flex items-center gap-2 text-blue-600 dark:text-blue-400 font-semibold mb-1">
                            <FiPlusCircle /> 추가(Add)
                        </div>
                        <p className="text-gray-700 dark:text-gray-300 mb-2">
                            감정은 수정 없이 말한 순서대로 추가됩니다.
                        </p>
                        <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg mb-3 leading-relaxed">
                            예시:<br />
                            • “오늘 기분 좋아”<br />
                            • “근데 오후에 스트레스 좀 받았어”
                        </div>
                    </div>

                    <p className="text-gray-700 dark:text-gray-300 mb-2">
                        AI가 감정의 원인·톤·강도 등을 분석합니다.
                    </p>
                </div>
                {/* 🔹 간단한 계산 방식 섹션 (React Icons 적용) */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold text-gray-800 dark:text-gray-200 mb-3 flex items-center gap-2">
                        <FiInfo /> 분석 방식 안내
                    </h3>

                    {/* 식단 */}
                    <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg mb-3 leading-relaxed">
                        <strong className="flex items-center gap-2">
                            <PiBowlFoodBold className="text-blue-600" /> 식단 칼로리 계산
                        </strong>
                        음식 이름과 양을 분석해, LLM이 학습한 표준 칼로리 정보를 기반으로 추정합니다.
                        <br />
                        예: “김밥 한 줄” → 기준 칼로리 × 양 비율
                    </div>

                    {/* 운동 */}
                    <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg mb-3 leading-relaxed">
                        <strong className="flex items-center gap-2">
                            <FiActivity className="text-green-600" /> 운동 칼로리 계산
                        </strong>
                        운동 종류(MET), 체중, 운동 시간을 활용해 칼로리를 계산합니다.
                        <br />
                        예: 조깅 30분 → MET × 체중 × 시간
                    </div>

                    {/* 감정 */}
                    <div className="text-sm bg-gray-100 dark:bg-gray-700 p-3 rounded-lg leading-relaxed">
                        <strong className="flex items-center gap-2">
                            <FiSmile className="text-pink-500" /> 감정 분석
                        </strong>
                        문장의 감정 톤을 분석해 주요 감정과 강도를 기록합니다.
                        <br />
                        예: “스트레스 받았어” → 감정: 스트레스 / 강도 높음
                    </div>
                </div>


                {/* 🔹 삭제 */}
                <div className="mb-8">
                    <h3 className="text-xl font-semibold flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-3">
                        <FiTrash /> 전체 삭제 명령어
                    </h3>

                    <p className="text-gray-700 dark:text-gray-300 mb-2">
                        아래 문장을 입력하면 오늘 기록 전체 또는 카테고리별 삭제가 가능합니다.
                    </p>

                    <div className="text-sm bg-gray-100 dark:bg-gray-700 p-4 rounded-lg leading-relaxed">
                        <strong>전체 기록 삭제</strong><br />
                        “오늘 기록 전체 삭제”, “전체 초기화”, “전부 다 지워”
                        <br /><br />

                        <strong>식단 삭제</strong><br />
                        “식단 삭제”, “오늘 식단 초기화”, “먹은거 삭제”
                        <br /><br />

                        <strong>운동 삭제</strong><br />
                        “운동 삭제”, “운동 전부 삭제”
                        <br /><br />

                        <strong>감정 삭제</strong><br />
                        “감정 삭제”, “오늘 감정 삭제”
                    </div>
                </div>

                {/* 푸터 */}
                <p className="text-center text-gray-600 dark:text-gray-400 text-sm mt-6">
                    언제든 자연어로 입력하세요. AI가 자동으로 이해하고 기록해줍니다!
                </p>

                <div className="text-center mt-6">
                    <button
                        onClick={onClose}
                        className="px-5 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition"
                    >
                        닫기
                    </button>
                </div>
            </motion.div>
        </div>
    );
}
