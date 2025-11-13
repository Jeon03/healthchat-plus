import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from "recharts";

type Props = {
    protein: number;
    fat: number;
    carbs: number;
    size?: number;
};

export default function NutritionDonut({
                                           protein,
                                           fat,
                                           carbs,
                                           size = 340,   // ⬅ 기본값을 더 크게 변경
                                       }: Props) {

    const total = (protein + fat + carbs) || 1;

    const data = [
        { name: "단백질", value: protein },
        { name: "지방", value: fat },
        { name: "탄수화물", value: carbs },
    ];

    const COLORS = ["#60a5fa", "#f59e0b", "#10b981"];

    return (
        <div className="relative w-full" style={{ height: size }}>
            <ResponsiveContainer>
                <PieChart>
                    <Pie
                        data={data}
                        dataKey="value"
                        nameKey="name"
                        innerRadius="70%"   // ⬅ 기존 60 → 62
                        outerRadius="100%"   // ⬅ 기존 85 → 88
                        strokeWidth={2}
                        isAnimationActive
                    >
                        {data.map((_, i) => (
                            <Cell key={i} fill={COLORS[i % COLORS.length]} />
                        ))}
                    </Pie>

                    <Tooltip
                        formatter={(value: any, _name: string, item: any) => {
                            const pct = ((value / total) * 100).toFixed(0);
                            return [`${value.toFixed(1)} g (${pct}%)`, item.payload.name];
                        }}
                    />
                </PieChart>
            </ResponsiveContainer>

            {/* 중앙 요약 */}
            <div className="absolute inset-0 flex flex-col items-center justify-center select-none pointer-events-none">
                <div className="text-sm text-gray-500 dark:text-gray-400">총 영양소</div>
                <div className="text-2xl font-semibold text-gray-900 dark:text-gray-100">
                    {(protein + fat + carbs).toFixed(1)} g
                </div>
            </div>

            {/* 범례 */}
            <div className="mt-4 flex justify-center gap-6 text-sm text-gray-600 dark:text-gray-300">
                <span className="flex items-center gap-1">
                    <span className="inline-block w-3 h-3 rounded-full" style={{ background: COLORS[0] }} />
                    단백질
                </span>
                <span className="flex items-center gap-1">
                    <span className="inline-block w-3 h-3 rounded-full" style={{ background: COLORS[1] }} />
                    지방
                </span>
                <span className="flex items-center gap-1">
                    <span className="inline-block w-3 h-3 rounded-full" style={{ background: COLORS[2] }} />
                    탄수화물
                </span>
            </div>
        </div>
    );
}
