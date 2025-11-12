import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from "recharts";

type Props = {
    protein: number; // g
    fat: number;     // g
    carbs: number;   // g
    size?: number;   // optional
};

export default function NutritionDonut({ protein, fat, carbs, size = 240 }: Props) {
    const total = (protein + fat + carbs) || 1;

    const data = [
        { name: "단백질", value: protein },
        { name: "지방", value: fat },
        { name: "탄수화물", value: carbs },
    ];

    const COLORS = ["#60a5fa", "#f59e0b", "#10b981"]; // 파랑 / 노랑 / 초록

    return (
        <div className="relative w-full" style={{ height: size }}>
            <ResponsiveContainer>
                <PieChart>
                    <Pie
                        data={data}
                        dataKey="value"
                        nameKey="name"
                        innerRadius="60%"
                        outerRadius="85%"
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
                <div className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                    {(protein + fat + carbs).toFixed(1)} g
                </div>
            </div>

            {/* 범례 */}
            <div className="mt-3 flex justify-center gap-4 text-xs text-gray-600 dark:text-gray-300">
        <span className="flex items-center gap-1">
          <span className="inline-block w-2 h-2 rounded-full" style={{ background: COLORS[0] }} />
          단백질
        </span>
                <span className="flex items-center gap-1">
          <span className="inline-block w-2 h-2 rounded-full" style={{ background: COLORS[1] }} />
          지방
        </span>
                <span className="flex items-center gap-1">
          <span className="inline-block w-2 h-2 rounded-full" style={{ background: COLORS[2] }} />
          탄수화물
        </span>
            </div>
        </div>
    );
}
