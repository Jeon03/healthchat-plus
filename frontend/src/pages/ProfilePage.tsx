import { useEffect, useState, useMemo } from "react";
import api from "../api/axios";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import GoalModal from "../components/GoalModal";

export default function ProfilePage() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);

    // ✅ 목표 관련 상태
    const [step, setStep] = useState<"main" | "detail">("main");
    const [selectedGoals, setSelectedGoals] = useState<string[]>([]);
    const [goalDetails, setGoalDetails] = useState<any[]>([]); // [{goal, factors: []}]
    const [goalText, setGoalText] = useState(""); // 자유입력 목표 텍스트

    // ✅ 프로필 폼
    const [form, setForm] = useState({
        nickname: "",
        gender: "",
        birthDate: "",
        height: "",
        weight: "",
        allergiesText: "",
        medicationsText: "",
        goalWeight: "",
        avgSleep: "",
    });
    useEffect(() => {
        const hasCustom = selectedGoals.includes("기타 (직접 입력)");
        if (!hasCustom) {
            // 기타 텍스트 비우고, details에서 기타 항목 삭제
            setGoalText("");
            setGoalDetails((prev) => prev.filter((d: any) => d.goal !== "기타 (직접 입력)"));
        }
    }, [selectedGoals]);
    /** ✅ 기존 프로필 불러오기 */
    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/user/profile");
                console.log("📦 백엔드 응답 데이터:", res.data);

                if (res.data) {
                    // 1) 기본 폼 세팅
                    setForm((prev) => ({
                        ...prev,
                        ...Object.fromEntries(Object.entries(res.data).map(([k, v]) => [k, v ?? ""])),
                        birthDate: res.data.birthDate ?? "",
                    }));

                    // 2) 목표/세부요인 복원
                    let parsedDetails: Array<{ goal: string; factors: string[] }> = [];
                    try {
                        if (res.data.goalsDetailJson) {
                            parsedDetails = JSON.parse(res.data.goalsDetailJson);
                            if (!Array.isArray(parsedDetails)) parsedDetails = [];
                        }
                    } catch (e) {
                        console.warn("goalsDetailJson 파싱 실패:", e);
                        parsedDetails = [];
                    }
                    setGoalDetails(parsedDetails);

                    // 3) 선택된 목표 복원
                    const restoredGoals = parsedDetails.map((d) => d.goal);
                    setSelectedGoals(restoredGoals);

                    // 4) 자유입력 목표 복원 (기타 처리)
                    if (res.data.goalText && !res.data.goalsDetailJson) {
                        // 세부요인이 없고 자유 입력만 있는 케이스 → '기타' 상태로 복원
                        setGoalText(res.data.goalText);
                        setSelectedGoals(["기타 (직접 입력)"]);
                    } else if (res.data.goalText) {
                        // 세부요인도 있고 추가 설명도 있는 케이스 → 그냥 텍스트만 세팅
                        setGoalText(res.data.goalText);
                    }
                }
            } catch {
                console.warn("⚠️ 프로필 정보가 없습니다. 새로 작성합니다.");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    /** ✅ 나이 계산 */
    const age = useMemo(() => {
        if (!form.birthDate) return "";
        const birth = new Date(form.birthDate);
        const today = new Date();
        let calculated = today.getFullYear() - birth.getFullYear();
        const monthDiff = today.getMonth() - birth.getMonth();
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
            calculated--;
        }
        return calculated;
    }, [form.birthDate]);

    /** ✅ 입력 변경 처리 */
    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
    ) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleGoalSave = (details: any[], text: string) => {
        console.log("🎯 목표 저장됨 (새로운 details):", details, text);

        const weightGoals = ["체중 감량", "체중 유지", "체중 증가"];
        const hasCustomGoal = details.some((d) => d.goal === "기타 (직접 입력)");

        // 1️⃣ '기타' 단독 처리
        if (hasCustomGoal) {
            setGoalDetails([{ goal: "기타 (직접 입력)", factors: [text] }]);
            setGoalText(text);
            setIsGoalModalOpen(false);
            return;
        }

        // 2️⃣ 체중 관련 목표는 1개만
        const uniqueWeight = details.filter((d) => weightGoals.includes(d.goal));
        const weightGoal = uniqueWeight.length > 0 ? [uniqueWeight[0]] : [];

        // 3️⃣ 일반 목표
        const normalGoals = details.filter(
            (d) => !weightGoals.includes(d.goal) && d.goal !== "기타 (직접 입력)"
        );

        // 4️⃣ 최종 병합 (체중 1개 + 일반 ≤3개)
        const merged = [...weightGoal, ...normalGoals].slice(0, 3);

        // 5️⃣ 상태 업데이트
        setGoalDetails(merged);
        setGoalText("");
        setIsGoalModalOpen(false);

        console.log("✅ 최종 저장된 goalDetails:", merged);
    };


    /** ✅ 프로필 저장 */
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // ⚠️ 목표 미설정 시 저장 차단
        if (goalDetails.length === 0 && !goalText.trim()) {
            toast.error("목표를 설정해야 프로필을 저장할 수 있습니다 🎯");
            return;
        }

        // ✅ 전송할 payload
        const payload = {
            ...form,
            goalsDetailJson: JSON.stringify(goalDetails),
            goalText: goalText,
        };

        console.log("📤 [백엔드 전송 데이터]");
        console.log(JSON.stringify(payload, null, 2));

        try {
            await api.post("/user/profile", payload);
            toast.success("프로필이 저장되었습니다 🎉");
            setTimeout(() => navigate("/dashboard"), 1500);
        } catch (err) {
            console.error("❌ 프로필 저장 실패:", err);
            toast.error("저장 실패 😢 다시 시도해주세요");
        }
    };

    const toggleGoal = (goal: string) => {
        const weightGoals = ["체중 감량", "체중 유지", "체중 증가"];

        // ✅ 1️⃣ 기타 (직접 입력)
        if (goal === "기타 (직접 입력)") {
            if (selectedGoals.includes("기타 (직접 입력)")) {
                // 이미 선택된 경우 → 해제
                setSelectedGoals([]);
            } else {
                // 새로 선택 → 다른 모든 목표 해제 후 단독 선택
                setSelectedGoals(["기타 (직접 입력)"]);
            }
            return;
        }

        // ✅ 2️⃣ 기타가 이미 선택된 상태에서 일반 목표 클릭 → 기타 해제 후 일반 목표 선택
        if (selectedGoals.includes("기타 (직접 입력)")) {
            setSelectedGoals([goal]);
            return;
        }

        // ✅ 3️⃣ 체중 관련 목표 (감량·유지·증가)는 하나만 선택 가능
        if (weightGoals.includes(goal)) {
            const filtered = selectedGoals.filter(
                (g) => !weightGoals.includes(g)
            );
            if (selectedGoals.includes(goal)) {
                // 이미 선택된 체중 목표 다시 클릭 시 해제
                setSelectedGoals(filtered);
            } else {
                // 새로운 체중 목표 선택
                setSelectedGoals([...filtered, goal]);
            }
            return;
        }

        // ✅ 4️⃣ 일반 목표 (최대 3개 제한, 다시 클릭 시 해제)
        if (selectedGoals.includes(goal)) {
            // 다시 클릭 시 해제
            setSelectedGoals(selectedGoals.filter((g) => g !== goal));
        } else if (selectedGoals.length < 3) {
            // 3개 이하일 때 추가 가능
            setSelectedGoals([...selectedGoals, goal]);
        }
    };


    const handleNext = () => setStep("detail");

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen text-gray-500">
                프로필 정보를 불러오는 중입니다...
            </div>
        );
    }

    /** ✅ 저장 버튼 비활성화 조건 */
    const isSaveDisabled =
        !form.nickname ||
        !form.gender ||
        !form.birthDate ||
        !form.height ||
        !form.weight ||
        (goalDetails.length === 0 && !goalText.trim());

    return (
        <div className="px-6 py-10 max-w-xl mx-auto">
            <h2 className="text-3xl font-bold mb-10 text-gray-800 dark:text-gray-100">
                프로필 설정
            </h2>

            <form onSubmit={handleSubmit} className="space-y-6">
                {/* ✅ 닉네임 */}
                <div>
                    <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                        닉네임
                    </label>
                    <input
                        name="nickname"
                        value={form.nickname}
                        onChange={handleChange}
                        className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                        required
                    />
                </div>

                {/* ✅ 성별 */}
                <div>
                    <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                        성별
                    </label>
                    <select
                        name="gender"
                        value={form.gender}
                        onChange={handleChange}
                        className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                        required
                    >
                        <option value="">성별 선택</option>
                        <option value="M">남성</option>
                        <option value="F">여성</option>
                        <option value="OTHER">기타</option>
                    </select>
                </div>

                {/* ✅ 생년월일 + 나이 */}
                <div>
                    <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                        생년월일
                    </label>
                    <div className="flex items-center gap-4">
                        <input
                            name="birthDate"
                            type="date"
                            value={form.birthDate || ""}
                            onChange={handleChange}
                            className="flex-1 border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                            required
                        />
                        {age && (
                            <span className="text-gray-600 dark:text-gray-300 text-sm">
                                만 {age}세
                            </span>
                        )}
                    </div>
                </div>

                {/* ✅ 키 / 몸무게 / 목표 체중 / 수면 */}
                {[
                    { name: "height", label: "키", unit: "cm", required: true },
                    { name: "weight", label: "몸무게", unit: "kg", required: true },
                    { name: "goalWeight", label: "목표 체중", unit: "kg" },
                    { name: "avgSleep", label: "평균 수면 시간", unit: "시간" },
                ].map(({ name, label, unit, required }) => (
                    <div key={name}>
                        <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                            {label}
                        </label>
                        <div className="relative">
                            <input
                                name={name}
                                type="number"
                                value={(form as any)[name]}
                                onChange={handleChange}
                                className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 pr-12 bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                                required={required}
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500">
                                {unit}
                            </span>
                        </div>
                    </div>
                ))}

                {/* ✅ 알레르기 정보 */}
                <div>
                    <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                        알레르기 정보
                    </label>
                    <textarea
                        name="allergiesText"
                        value={form.allergiesText}
                        onChange={handleChange}
                        placeholder="예: 우유, 계란, 새우 알레르기 있음"
                        className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 h-24 resize-none bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                    />
                </div>

                {/* ✅ 복용 중인 약 */}
                <div>
                    <label className="block mb-2 text-gray-700 dark:text-gray-300 font-medium">
                        복용 중인 약
                    </label>
                    <textarea
                        name="medicationsText"
                        value={form.medicationsText}
                        onChange={handleChange}
                        placeholder="예: 고혈압약, 비타민 D, 오메가3"
                        className="w-full border border-gray-300 dark:border-gray-700 rounded-md px-3 py-2 h-24 resize-none bg-white dark:bg-gray-800 focus:ring-2 focus:ring-blue-500 outline-none"
                    />
                </div>
                {/* ✅ 나의 목표 요약 */}
                {(goalDetails.length > 0 || goalText.trim()) && (
                    <div className="mt-10 p-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                        <h3 className="text-xl font-semibold mb-4 text-gray-800 dark:text-gray-100">
                            🎯 나의 목표
                        </h3>

                        {/* ✅ 기타(직접 입력) */}
                        {goalDetails.some((g) => g.goal === "기타 (직접 입력)") ? (
                            <p className="text-gray-700 dark:text-gray-300 text-base whitespace-pre-line leading-relaxed">
                                {goalText || "직접 입력한 목표가 없습니다."}
                            </p>
                        ) : (
                            /* ✅ 일반 목표 */
                            <div className="space-y-4">
                                {goalDetails.map(({ goal, factors }, idx) => (
                                    <div key={idx}>
                                        <p className="font-medium text-blue-600 dark:text-blue-400 mb-2">
                                            • {goal}
                                        </p>
                                        {factors && factors.length > 0 && (
                                            <ul className="list-disc list-inside text-gray-700 dark:text-gray-300 text-sm leading-relaxed">
                                                {factors.map((f: string, i: number) => (
                                                    <li key={i}>{f}</li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* ✅ 버튼 영역 */}
                <div className="mt-8">
                    <button
                        type="button"
                        onClick={() => setIsGoalModalOpen(true)}
                        className="w-full px-5 py-3 bg-green-600 text-white rounded-md font-medium hover:bg-green-700 transition mb-4"
                    >
                        목표 설정하기
                    </button>
                    <div className="grid grid-cols-2 gap-3">
                        <button
                            type="button"
                            onClick={() => navigate("/dashboard")}
                            className="px-5 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition"
                        >
                            취소
                        </button>

                        <button
                            type="submit"
                            disabled={isSaveDisabled}
                            className={`px-5 py-2 rounded-md font-medium transition ${
                                isSaveDisabled
                                    ? "bg-gray-400 text-white cursor-not-allowed"
                                    : "bg-blue-600 text-white hover:bg-blue-700"
                            }`}
                        >
                            저장하기
                        </button>
                    </div>
                </div>
            </form>

            {/* ✅ GoalModal */}
            {isGoalModalOpen && (
                <GoalModal
                    step={step}
                    setStep={setStep}
                    selectedGoals={selectedGoals}
                    setSelectedGoals={setSelectedGoals}
                    toggleGoal={toggleGoal}
                    customGoal={goalText}
                    setCustomGoal={setGoalText}
                    handleNext={handleNext}
                    onClose={() => setIsGoalModalOpen(false)}
                    onSave={handleGoalSave}
                    existingDetails={goalDetails}
                />
            )}
        </div>
    );
}
