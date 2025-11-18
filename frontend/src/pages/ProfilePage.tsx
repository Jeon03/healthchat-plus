import { useEffect, useMemo, useState } from "react";
import api from "../api/axios";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import GoalModal from "../components/GoalModal";
import { motion } from "framer-motion";

import {
    LuUser,
    LuCalendar,
    LuRuler,
    LuWeight,
    LuTarget,
    LuPill,
    LuDna,
    LuAlarmClock,
} from "react-icons/lu";

/* ----------------------------------
   🎨 Section Title (Scroll Animation)
----------------------------------- */
function SectionTitle({
                          icon,
                          title,
                      }: {
    icon: React.ReactNode;
    title: string;
}) {
    return (
        <motion.h3
            initial={{ opacity: 0, x: -10 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.4, ease: "easeOut" }}
            className="flex items-center gap-2 text-xl font-semibold mb-2 text-gray-800 dark:text-gray-200"
        >
            {icon}
            {title}
        </motion.h3>
    );
}

export default function ProfilePage() {
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);

    const [step, setStep] = useState<"main" | "detail">("main");
    const [selectedGoals, setSelectedGoals] = useState<string[]>([]);
    const [goalDetails, setGoalDetails] = useState<any[]>([]);
    const [goalText, setGoalText] = useState("");

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

    /* 기타 목표 자동 정리 */
    useEffect(() => {
        const hasCustom = selectedGoals.includes("기타 (직접 입력)");
        if (!hasCustom) {
            setGoalText("");
            setGoalDetails((prev) =>
                prev.filter((d: any) => d.goal !== "기타 (직접 입력)")
            );
        }
    }, [selectedGoals]);

    /* 프로필 불러오기 */
    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/user/profile");

                if (res.data) {
                    setForm((prev) => ({
                        ...prev,
                        ...Object.fromEntries(
                            Object.entries(res.data).map(([k, v]) => [k, v ?? ""])
                        ),
                        birthDate: res.data.birthDate ?? "",
                    }));

                    let parsed: Array<{ goal: string; factors: string[] }> = [];
                    try {
                        if (res.data.goalsDetailJson) {
                            parsed = JSON.parse(res.data.goalsDetailJson);
                            if (!Array.isArray(parsed)) parsed = [];
                        }
                    } catch {
                        parsed = [];
                    }

                    setGoalDetails(parsed);
                    setSelectedGoals(parsed.map((d) => d.goal));

                    if (res.data.goalText && !res.data.goalsDetailJson) {
                        setGoalText(res.data.goalText);
                        setSelectedGoals(["기타 (직접 입력)"]);
                    } else if (res.data.goalText) {
                        setGoalText(res.data.goalText);
                    }
                }
            } catch {
                console.warn("프로필 없음 → 신규 작성");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    /* 나이 계산 */
    const age = useMemo(() => {
        if (!form.birthDate) return "";
        const birth = new Date(form.birthDate);
        const today = new Date();
        let a = today.getFullYear() - birth.getFullYear();
        const md = today.getMonth() - birth.getMonth();
        if (md < 0 || (md === 0 && today.getDate() < birth.getDate())) a--;
        return a;
    }, [form.birthDate]);

    /* 값 변경 */
    const handleChange = (
        e: React.ChangeEvent<
            HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
        >
    ) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    /* 목표 저장 */
    const handleGoalSave = (details: any[], text: string) => {
        const weightGoals = ["체중 감량", "체중 유지", "체중 증가"];
        const hasCustomGoal = details.some(
            (d) => d.goal === "기타 (직접 입력)"
        );

        if (hasCustomGoal) {
            setGoalDetails([{ goal: "기타 (직접 입력)", factors: [text] }]);
            setGoalText(text);
            setIsGoalModalOpen(false);
            return;
        }

        const uniqueWeight = details.filter((d) =>
            weightGoals.includes(d.goal)
        );
        const weightGoal = uniqueWeight.length > 0 ? [uniqueWeight[0]] : [];

        const normalGoals = details.filter(
            (d) =>
                !weightGoals.includes(d.goal) &&
                d.goal !== "기타 (직접 입력)"
        );

        const merged = [...weightGoal, ...normalGoals].slice(0, 3);

        setGoalDetails(merged);
        setGoalText("");
        setIsGoalModalOpen(false);
    };

    /* 저장 */
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (goalDetails.length === 0 && !goalText.trim()) {
            toast.error("목표를 설정해야 저장할 수 있습니다 🎯");
            return;
        }

        const payload = {
            ...form,
            goalsDetailJson: JSON.stringify(goalDetails),
            goalText,
        };

        try {
            await api.post("/user/profile", payload);
            toast.success("프로필이 저장되었습니다 🎉");
            setTimeout(() => navigate("/dashboard"), 1200);
        } catch {
            toast.error("저장 실패. 다시 시도해주세요.");
        }
    };

    /* 목표 선택 */
    const toggleGoal = (goal: string) => {
        const weightGoals = ["체중 감량", "체중 유지", "체중 증가"];

        if (goal === "기타 (직접 입력)") {
            if (selectedGoals.includes(goal)) setSelectedGoals([]);
            else setSelectedGoals([goal]);
            return;
        }

        if (selectedGoals.includes("기타 (직접 입력)")) {
            setSelectedGoals([goal]);
            return;
        }

        if (weightGoals.includes(goal)) {
            const filtered = selectedGoals.filter(
                (g) => !weightGoals.includes(g)
            );
            if (selectedGoals.includes(goal)) setSelectedGoals(filtered);
            else setSelectedGoals([...filtered, goal]);
            return;
        }

        if (selectedGoals.includes(goal)) {
            setSelectedGoals(selectedGoals.filter((g) => g !== goal));
        } else if (selectedGoals.length < 3) {
            setSelectedGoals([...selectedGoals, goal]);
        }
    };

    const isSaveDisabled =
        !form.nickname ||
        !form.gender ||
        !form.birthDate ||
        !form.height ||
        !form.weight ||
        (goalDetails.length === 0 && !goalText.trim());

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen text-gray-500">
                프로필 정보를 불러오는 중입니다...
            </div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.45, ease: "easeOut" }}
            className="px-6 py-12 max-w-2xl mx-auto"
        >
            {/* 제목 */}
            <motion.h2
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, delay: 0.05 }}
                className="text-3xl font-bold mb-10 text-gray-800 dark:text-gray-100 tracking-tight"
            >
                프로필 설정
            </motion.h2>

            {/* 메인 카드 */}
            <motion.form
                onSubmit={handleSubmit}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.55, delay: 0.1 }}
                className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm p-8 space-y-8"
            >
                {/* 닉네임 */}
                <div>
                    <SectionTitle icon={<LuUser className="text-blue-500" />} title="닉네임" />
                    <input
                        name="nickname"
                        value={form.nickname}
                        onChange={handleChange}
                        className="w-full px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none"
                        required
                    />
                </div>

                {/* 성별 */}
                <div>
                    <SectionTitle icon={<LuUser className="text-pink-500" />} title="성별" />
                    <select
                        name="gender"
                        value={form.gender}
                        onChange={handleChange}
                        className="w-full px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none"
                        required
                    >
                        <option value="">성별 선택</option>
                        <option value="M">남성</option>
                        <option value="F">여성</option>
                        <option value="OTHER">기타</option>
                    </select>
                </div>

                {/* 생년월일 */}
                <div>
                    <SectionTitle icon={<LuCalendar className="text-indigo-500" />} title="생년월일" />
                    <div className="flex items-center gap-4">
                        <input
                            name="birthDate"
                            type="date"
                            value={form.birthDate}
                            onChange={handleChange}
                            className="flex-1 px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none"
                            required
                        />
                        {age && (
                            <span className="text-gray-600 dark:text-gray-300 text-sm">
                                만 {age}세
                            </span>
                        )}
                    </div>
                </div>

                {/* 키/몸무게/목표체중/평균수면 */}
                {[
                    { name: "height", label: "키", unit: "cm", icon: <LuRuler /> },
                    { name: "weight", label: "몸무게", unit: "kg", icon: <LuWeight /> },
                    { name: "goalWeight", label: "목표 체중", unit: "kg", icon: <LuTarget /> },
                    { name: "avgSleep", label: "평균 수면", unit: "시간", icon: <LuAlarmClock /> },
                ].map(({ name, label, unit, icon }) => (
                    <div key={name}>
                        <SectionTitle icon={icon} title={label} />
                        <motion.div
                            initial={{ opacity: 0, y: 10 }}
                            whileInView={{ opacity: 1, y: 0 }}
                            viewport={{ once: true }}
                            transition={{ duration: 0.4 }}
                            className="relative"
                        >
                            <input
                                type="number"
                                name={name}
                                value={(form as any)[name]}
                                onChange={handleChange}
                                className="w-full px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none pr-12"
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500">
                                {unit}
                            </span>
                        </motion.div>
                    </div>
                ))}

                {/* 알레르기 */}
                <div>
                    <SectionTitle icon={<LuDna className="text-orange-500" />} title="알레르기" />
                    <textarea
                        name="allergiesText"
                        value={form.allergiesText}
                        onChange={handleChange}
                        className="w-full h-24 px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                    />
                </div>

                {/* 복용약 */}
                <div>
                    <SectionTitle icon={<LuPill className="text-green-500" />} title="복용 중인 약" />
                    <textarea
                        name="medicationsText"
                        value={form.medicationsText}
                        onChange={handleChange}
                        className="w-full h-24 px-4 py-2 rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                    />
                </div>

                {/* 목표 요약 */}
                {(goalDetails.length > 0 || goalText.trim()) && (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.97 }}
                        whileInView={{ opacity: 1, scale: 1 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.4 }}
                        className="p-5 rounded-xl bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700"
                    >
                        <SectionTitle icon={<LuTarget className="text-pink-500" />} title="나의 목표" />

                        {goalDetails.some((g) => g.goal === "기타 (직접 입력)") ? (
                            <p className="text-gray-700 dark:text-gray-300 leading-relaxed">
                                {goalText}
                            </p>
                        ) : (
                            <div className="space-y-4">
                                {goalDetails.map(({ goal, factors }: any, i: number) => (
                                    <div key={i}>
                                        <p className="font-medium text-blue-600 dark:text-blue-400 mb-1">
                                            • {goal}
                                        </p>
                                        {factors?.length > 0 && (
                                            <ul className="list-disc list-inside text-gray-700 dark:text-gray-300 text-sm">
                                                {factors.map((f: string, idx: number) => (
                                                    <li key={idx}>{f}</li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </motion.div>
                )}

                {/* 버튼 영역 */}
                <div className="pt-4 space-y-4">
                    {/* 목표 설정 버튼 */}
                    <button
                        type="button"
                        onClick={() => setIsGoalModalOpen(true)}
                        className="
                            w-full px-5 py-3 rounded-xl font-semibold
                            bg-gradient-to-r from-indigo-600 via-blue-600 to-indigo-600
                            text-white shadow-lg shadow-blue-500/30
                            hover:shadow-blue-400/50 hover:scale-[1.02]
                            active:scale-95 transition-all duration-300
                        "
                    >
                        목표 설정하기
                    </button>

                    <div className="grid grid-cols-2 gap-3">
                        {/* 취소 */}
                        <button
                            type="button"
                            onClick={() => navigate("/dashboard")}
                            className="
                                px-5 py-3 rounded-xl font-medium
                                bg-white/60 dark:bg-gray-800/40
                                border border-gray-300/40 dark:border-gray-700/40
                                text-gray-700 dark:text-gray-200
                                backdrop-blur-md
                                hover:bg-white/80 dark:hover:bg-gray-700/50
                                transition-all duration-300
                            "
                        >
                            취소
                        </button>

                        {/* 저장 */}
                        <button
                            type="submit"
                            disabled={isSaveDisabled}
                            className={`
                                px-5 py-3 rounded-xl font-semibold transition-all duration-300
                                ${
                                isSaveDisabled
                                    ? "bg-gray-300 dark:bg-gray-700 text-gray-500 cursor-not-allowed"
                                    : `
                                    bg-gradient-to-r from-indigo-600 via-blue-600 to-indigo-600
                                    text-white shadow-lg shadow-blue-500/30
                                    hover:shadow-blue-400/50 hover:scale-[1.02]
                                `
                            }
                            `}
                        >
                            저장하기
                        </button>
                    </div>
                </div>
            </motion.form>

            {/* 목표 모달 */}
            {isGoalModalOpen && (
                <GoalModal
                    step={step}
                    setStep={setStep}
                    selectedGoals={selectedGoals}
                    setSelectedGoals={setSelectedGoals}
                    toggleGoal={toggleGoal}
                    customGoal={goalText}
                    setCustomGoal={setGoalText}
                    handleNext={() => setStep("detail")}
                    existingDetails={goalDetails}
                    onSave={handleGoalSave}
                    onClose={() => setIsGoalModalOpen(false)}
                />
            )}
        </motion.div>
    );
}
