import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts";

interface Props {
    data: { category: string; part: string; total: number }[];
}

const COLORS = [
    "rgb(96,165,250)",
    "rgb(167,139,250)",
    "rgb(248,180,217)",
    "rgb(74,222,128)",
    "rgb(250,204,21)",
    "rgb(156,163,175)"
];

const KOR_CATEGORY: Record<string, string> = {
    CARDIO: "유산소",
    HEALTH: "근력",
    STRENGTH: "근력",
    YOGA: "요가",
    PILATES: "필라테스",
    STRETCHING: "스트레칭",
    OTHER: "기타"
};

const KOR_PART: Record<string, string> = {
    CHEST: "가슴",
    BACK: "등",
    SHOULDER: "어깨",
    LOWER: "하체",
    ABS: "복근",
    FULL: "전신",
    OTHER: "기타"
};

export default function CategoryDonutWithPartChart({ data }: Props) {

    // === 카테고리 그룹 ===
    const categoryMap: Record<string, number> = {};
    data.forEach((item) => {
        categoryMap[item.category] = (categoryMap[item.category] || 0) + item.total;
    });

    const categoryArray = Object.entries(categoryMap).map(([category, total]) => ({
        category,
        total
    }));

    // === 부위 그룹 ===
    const partMap: Record<string, number> = {};
    data.forEach((item) => {
        partMap[item.part] = (partMap[item.part] || 0) + item.total;
    });

    const partArray = Object.entries(partMap).map(([part, total]) => ({
        part,
        total
    }));

    // 전체 kcal
    const totalKcal = categoryArray.reduce((a, b) => a + b.total, 0);

    // 퍼센트 변환
    const categoryPercent = categoryArray.map((d) => ({
        ...d,
        percent: totalKcal === 0 ? 0 : Math.round((d.total / totalKcal) * 100)
    }));

    const partPercent = partArray.map((d) => ({
        ...d,
        percent: totalKcal === 0 ? 0 : Math.round((d.total / totalKcal) * 100)
    }));

    return (
        <div className="w-full flex flex-col lg:flex-row gap-10 justify-center items-start">

            {/* =======================
                1) 운동 카테고리 도넛 차트
            ======================= */}
            <div className="bg-white dark:bg-gray-900 p-6 rounded-xl shadow-lg
                            border border-gray-200 dark:border-gray-700 w-full max-w-[360px] mx-auto">

                <h3 className="text-center mb-4 font-bold text-gray-700 dark:text-gray-200">
                    🏷 운동 카테고리 비율
                </h3>

                {/* ⭐ 높이 통일: 320px */}
                <div className="w-full h-[320px] flex justify-center">
                    <ResponsiveContainer width="90%" height="100%">
                        <PieChart>
                            <Pie
                                data={categoryPercent}
                                dataKey="percent"
                                nameKey="category"
                                cx="50%"
                                cy="50%"
                                innerRadius={70}
                                outerRadius={115}
                                paddingAngle={3}
                            >
                                {categoryPercent.map((_, index) => (
                                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* 퍼센트로 범례 표시 */}
                <div className="space-y-1 text-sm mt-2">
                    {categoryPercent.map((d, i) => (
                        <div key={i} className="flex items-center gap-2">
                            <span
                                className="inline-block w-3 h-3 rounded-sm"
                                style={{ backgroundColor: COLORS[i] }}
                            />
                            <span>{KOR_CATEGORY[d.category] || d.category}</span>
                            <span className="text-gray-500">
                                — {d.percent}% ({d.total} kcal)
                            </span>
                        </div>
                    ))}
                </div>
            </div>


            {/* =======================
                2) 운동 부위 도넛 차트
            ======================= */}
            <div className="bg-white dark:bg-gray-900 p-6 rounded-xl shadow-lg
                            border border-gray-200 dark:border-gray-700 w-full max-w-[360px] mx-auto">

                <h3 className="text-center mb-4 font-bold text-gray-700 dark:text-gray-200">
                    💪 운동 부위 비율
                </h3>

                {/* ⭐ 높이 통일 */}
                <div className="w-full h-[320px] flex justify-center">
                    <ResponsiveContainer width="90%" height="100%">
                        <PieChart>
                            <Pie
                                data={partPercent}
                                dataKey="percent"
                                nameKey="part"
                                cx="50%"
                                cy="50%"
                                innerRadius={70}
                                outerRadius={115}
                                paddingAngle={3}
                            >
                                {partPercent.map((_, index) => (
                                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* 퍼센트로 범례 표시 */}
                <div className="space-y-1 text-sm mt-2">
                    {partPercent.map((d, i) => (
                        <div key={i} className="flex items-center gap-2">
                            <span
                                className="inline-block w-3 h-3 rounded-sm"
                                style={{ backgroundColor: COLORS[i] }}
                            />
                            <span>{KOR_PART[d.part] || d.part}</span>
                            <span className="text-gray-500">
                                — {d.percent}% ({d.total} kcal)
                            </span>
                        </div>
                    ))}
                </div>
            </div>

        </div>
    );
}
