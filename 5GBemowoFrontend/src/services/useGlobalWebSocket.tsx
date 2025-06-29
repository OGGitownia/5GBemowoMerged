import { useEffect, useRef } from "react";
import { Message } from "../types/Message";
import { useUser } from "./UserContext.tsx";

export const useGlobalWebSocket = (onMessage: (msg: Message) => void) => {
    const { user } = useUser();
    const socketRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        if (!user?.id) {
            console.log("User ID is missing!");
            socketRef.current?.close();
            socketRef.current = null;
            return;
        }
        console.log("User ID is present!");

        const socket = new WebSocket(`${import.meta.env.VITE_API_URL}/ws/messages?userId=${user.id}`);
        socketRef.current = socket;

        socket.onopen = () => {
            console.log("WebSocket connected for user:", user.id);
        };

        socket.onmessage = (event) => {
            try {
                console.log("msg")
                const msg: Message = JSON.parse(event.data);
                console.log(msg)
                onMessage(msg);
            } catch (e) {
                console.error("WebSocket parse error:", e);
            }
        };

        socket.onclose = () => {
            console.log("🔌 WebSocket disconnected for user:", user.id);
        };

        socket.onerror = (e) => console.error("WebSocket error", e);

        return () => {
            socket.close();
            socketRef.current = null;
        };
    }, [user?.id]);
};
