import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts";

interface Props {
    emotions: string[];
    scores: number[];
}

/** 🎨 감정별 색상 매핑 */
const EMOTION_COLORS: Record<string, string> = {
    "기쁨": "#F472B6",      // 밝은 핑크
    "행복": "#F472B6",

    "우울": "#A78BFA",      // 보라
    "슬픔": "#A78BFA",

    "불안": "#FB923C",      // 오렌지
    "걱정": "#FB923C",

    "분노": "#F87171",      // 레드
    "짜증": "#F87171",

    "피곤": "#60A5FA",      // 블루
    "지침": "#60A5FA",

    "중립": "#A1A1AA",      // 그레이
    "무감정": "#A1A1AA",
};

/** 🎨 fallback 색상 */
const DEFAULT_COLORS = [
    "#FBCFE8",
    "#F9A8D4",
    "#F472B6",
    "#A78BFA",
    "#60A5FA",
    "#34D399",
];

export default function EmotionPieChart({ emotions, scores }: Props) {

    // 데이터가 없으면 렌더링 X
    if (!emotions || emotions.length === 0) return null;

    const total = scores.reduce((a, b) => a + b, 0);

    const data = emotions.map((emo, idx) => ({
        emotion: emo,
        score: scores[idx],
        percent: total === 0 ? 0 : Math.round((scores[idx] / total) * 100),
        color: EMOTION_COLORS[emo] || DEFAULT_COLORS[idx % DEFAULT_COLORS.length],
    }));

    return (
        <div
            className="
            bg-transparent
            dark:bg-transparent
            p-6 rounded-xl
            w-full max-w-[360px] mx-auto
        "
        >
            <h3 className="text-center mb-4 font-bold text-gray-800 dark:text-pink-200">
                감정 분포 차트
            </h3>

            {/* ⭐ 도넛 차트 */}
            <div className="w-full h-[320px] flex justify-center">
                <ResponsiveContainer width="90%" height="100%">
                    <PieChart>
                        <Pie
                            data={data}
                            dataKey="percent"
                            nameKey="emotion"
                            cx="50%"
                            cy="50%"
                            innerRadius={70}
                            outerRadius={115}
                            stroke="none"
                            paddingAngle={3}
                        >
                            {data.map((entry, i) => (
                                <Cell key={i} fill={entry.color} />
                            ))}
                        </Pie>
                    </PieChart>
                </ResponsiveContainer>
            </div>

            {/* ⭐ 범례 */}
            <div className="space-y-1 text-sm mt-2">
                {data.map((d, i) => (
                    <div key={i} className="flex items-center gap-2">
                        <span
                            className="inline-block w-3 h-3 rounded-sm"
                            style={{ backgroundColor: d.color }}
                        />
                        <span className="font-medium text-gray-800 dark:text-pink-200">
                            {d.emotion}
                        </span>
                        <span className="text-gray-500 dark:text-gray-400">
                            — {d.percent}% ({d.score}점)
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}
