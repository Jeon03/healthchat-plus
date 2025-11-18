import { useRef, useEffect } from "react";

interface Props {
    setWidth: (v: number) => void;
}

export default function ChatWrapper({ setWidth }: Props) {
    const resizing = useRef(false);

    useEffect(() => {
        const onMouseMove = (e: MouseEvent) => {
            if (!resizing.current) return;

            const newWidth = window.innerWidth - e.clientX;

            if (newWidth > 300 && newWidth < 720) {
                setWidth(newWidth);
            }
        };

        const stop = () => {
            resizing.current = false;
        };

        window.addEventListener("mousemove", onMouseMove);
        window.addEventListener("mouseup", stop);

        return () => {
            window.removeEventListener("mousemove", onMouseMove);
            window.removeEventListener("mouseup", stop);
        };
    }, [setWidth]);

    return (
        <div
            className="
                absolute left-0 top-0 w-1 h-full
                cursor-col-resize z-50 bg-transparent
            "
            onMouseDown={() => (resizing.current = true)}
        />
    );
}
