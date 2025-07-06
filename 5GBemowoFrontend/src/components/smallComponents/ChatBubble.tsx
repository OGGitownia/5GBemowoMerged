import React, { JSX } from "react";
import parse, { DOMNode, Text } from "html-react-parser";
import { Message } from "../../types/Message.tsx";
import "../../styles/smallComponents/ChatBubble.css";
import axios from "axios";
const BASE_URL = `${import.meta.env.VITE_API_URL}/api/photos`;

interface ChatBubbleProps {
    message: Message;
    onPreview: () => void;
}

const ChatBubble: React.FC<ChatBubbleProps> = ({ message, onPreview }) => {
    const baseId = message.baseId;

    const transform = (node: DOMNode, _index: number): JSX.Element | string | null => {
        if (node.type === "text") {
            const text = (node as Text).data;

            const regex = /photo_(\d+)\.(png|emf)/gi;
            const parts = [];
            let lastIndex = 0;
            let match;

           while ((match = regex.exec(text)) !== null) {
                const [fullMatch, photoId, ext] = match;
                const start = match.index;
                const end = regex.lastIndex;

                if (start > lastIndex) {
                    parts.push(text.slice(lastIndex, start));
                }

                const imageUrl = `${BASE_URL}/${baseId}/${fullMatch}`;
                parts.push(
                    <img
                        key={`${photoId}-${ext}-${start}`}
                        src={imageUrl}
                        alt={`photo ${photoId}`}
                        className="embedded-photo"
                    />
                );

                lastIndex = end;
            }


            if (lastIndex < text.length) {
                parts.push(text.slice(lastIndex));
            }

            return parts.length > 0 ? <>{parts}</> : text;
        }

        return null;
    };

    return (
        <div className="chat-bubble">
            <div className="user-question">
                <strong>You:</strong> {message.question}
            </div>
            <div className="ai-answer">
                <strong>AI:</strong>{" "}
                {message.answered ? parse(message.answer, { replace: transform }) : <em>Loading...</em>}
            </div>
            <div className="preview-context">
                <button onClick={onPreview}>Context Preview</button>
            </div>
        </div>
    );
};

export default ChatBubble;
