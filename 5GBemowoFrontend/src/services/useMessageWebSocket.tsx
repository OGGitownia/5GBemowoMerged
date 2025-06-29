import { useEffect } from "react";
import SockJS from "sockjs-client";
import { Client, over } from "stompjs";
import { Message } from "../types/Message";

let stompClient: Client | null = null;

export const useMessageWebSocket = (
    messageId: string,
    onAnswerReceived: (msg: Message) => void
) => {
    useEffect(() => {
        const socket = new SockJS(`${import.meta.env.VITE_API_URL}/ws`);
        stompClient = over(socket);

        stompClient.connect({}, () => {
            console.log("WebSocket connected");

            stompClient?.subscribe(`/topic/answer/${messageId}`, (frame) => {
                const body = JSON.parse(frame.body) as Message;
                onAnswerReceived(body);
            });
        });

        return () => {
            stompClient?.disconnect(() => console.log("WebSocket disconnected"));
        };
    }, [messageId, onAnswerReceived]);
};
