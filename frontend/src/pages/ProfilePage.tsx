import { useEffect, useState } from "react";
import api from "../api/axios";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";

export default function ProfilePage() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);

    const [form, setForm] = useState({
        nickname: "",
        gender: "",
        birthDate: "",
        height: "",
        weight: "",
        bodyFat: "",
        allergiesText: "",
        medicationsText: "",
        goalWeight: "",
        sleepGoal: "",
        avgSleep: "",
    });

    /** ✅ 기존 프로필 불러오기 */
    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/user/profile");
                console.log("📦 백엔드 응답 데이터:", res.data); // ✅ 콘솔로 확인

                if (res.data) {
                    // ✅ 데이터 세팅 (birthDate 포맷 포함)
                    setForm((prev) => ({
                        ...prev,
                        ...Object.fromEntries(
                            Object.entries(res.data).map(([k, v]) => [k, v ?? ""])
                        ),
                        birthDate: res.data.birthDate ?? "", // ✅ 명시적으로 설정
                    }));
                }
            } catch (err) {
                console.warn("⚠️ 프로필 정보가 아직 없습니다. 새로 입력합니다.");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    /** ✅ 폼 입력 처리 */
    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
    ) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    /** ✅ 저장 */
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await api.post("/user/profile", form);
            toast.success("프로필이 저장되었습니다 🎉");
            setTimeout(() => navigate("/dashboard"), 1500);
        } catch (err) {
            toast.error("저장 실패 😢 다시 시도해주세요");
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen text-gray-500">
                프로필 정보를 불러오는 중입니다...
            </div>
        );
    }

    return (
        <div className="max-w-lg mx-auto mt-10 bg-white dark:bg-gray-900 shadow-lg rounded-xl p-8">
            <h2 className="text-2xl font-bold mb-6 text-center">프로필 설정</h2>

            <form onSubmit={handleSubmit} className="space-y-4">
                {/* ✅ 닉네임 */}
                <input
                    name="nickname"
                    placeholder="닉네임"
                    value={form.nickname}
                    onChange={handleChange}
                    className="input"
                    required
                />

                {/* ✅ 성별 */}
                <select
                    name="gender"
                    value={form.gender}
                    onChange={handleChange}
                    className="input"
                    required
                >
                    <option value="">성별 선택</option>
                    <option value="M">남성</option>
                    <option value="F">여성</option>
                    <option value="OTHER">기타</option>
                </select>

                <input
                    name="birthDate"
                    type="date"
                    placeholder="생년월일"
                    value={form.birthDate || ""}
                    onChange={handleChange}
                    className="input"
                    required
                />

                {/* ✅ 키 / 몸무게 */}
                <input
                    name="height"
                    type="number"
                    placeholder="키 (cm)"
                    value={form.height}
                    onChange={handleChange}
                    className="input"
                    required
                />
                <input
                    name="weight"
                    type="number"
                    placeholder="몸무게 (kg)"
                    value={form.weight}
                    onChange={handleChange}
                    className="input"
                    required
                />

                <hr className="my-4 border-gray-300 dark:border-gray-700" />

                {/* ✅ 추가 정보 */}
                <input
                    name="bodyFat"
                    type="number"
                    placeholder="체지방률 (%)"
                    value={form.bodyFat}
                    onChange={handleChange}
                    className="input"
                />

                <input
                    name="goalWeight"
                    type="number"
                    placeholder="목표 체중 (kg)"
                    value={form.goalWeight}
                    onChange={handleChange}
                    className="input"
                />

                <input
                    name="sleepGoal"
                    type="number"
                    placeholder="수면 목표 시간 (시간)"
                    value={form.sleepGoal}
                    onChange={handleChange}
                    className="input"
                />

                <input
                    name="avgSleep"
                    type="number"
                    placeholder="평균 수면 시간 (시간)"
                    value={form.avgSleep}
                    onChange={handleChange}
                    className="input"
                />

                <textarea
                    name="allergiesText"
                    placeholder="알레르기 정보 (예: 우유, 계란, 새우 알레르기 있음)"
                    value={form.allergiesText}
                    onChange={handleChange}
                    className="input h-24 resize-none"
                />

                <textarea
                    name="medicationsText"
                    placeholder="복용 중인 약 (예: 고혈압약, 비타민D 복용 중)"
                    value={form.medicationsText}
                    onChange={handleChange}
                    className="input h-24 resize-none"
                />

                <button
                    type="submit"
                    className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
                >
                    저장하기
                </button>
            </form>
        </div>
    );
}
