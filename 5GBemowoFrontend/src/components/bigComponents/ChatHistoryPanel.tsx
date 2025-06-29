import React from "react";
import { useApp } from "../../services/AppContext.tsx";
import { Message } from "../../types/Message.tsx";
import { useNavigate } from "react-router-dom";
import "../../styles/bigComponents/ChatHistoryPanel.css";

const ChatHistoryPanel: React.FC = () => {
    const { chatMap, sortedChatIds, bases } = useApp();
    const navigate = useNavigate();

    const getLastMessage = (messages: Message[]): Message => {
        return messages.reduce((latest, msg) => {
            const latestTime = latest.answeredAt ?? latest.askedAt;
            const currentTime = msg.answeredAt ?? msg.askedAt;
            return currentTime > latestTime ? msg : latest;
        });
    };

    return (
        <div className="chat-history-container">
            <div className="chat-history-header">Chat History</div>
            <div className="chat-history-list">
                {sortedChatIds.slice().reverse().map(chatId => {
                    const messages = chatMap.get(chatId);
                    if (!messages || messages.length === 0) return null;
                    const lastMessage = getLastMessage(messages);
                    const base = bases.find(b => b.id.toString() === lastMessage.baseId);

                    if (!base) {
                        console.warn("Nie znaleziono bazy dla", lastMessage.baseId);
                        return null;
                    }

                    return (
                        <div
                            className="chat-tile"
                            key={chatId}
                            onClick={() => {
                                navigate("/chat", {
                                    state: {
                                        model: lastMessage.modelName,
                                        tuners: lastMessage.tuners,
                                        base,
                                        chatId
                                    }
                                });
                            }}
                            style={{ cursor: "pointer" }}
                        >
                            <div className="chat-info">
                                <div><strong>Model:</strong> {lastMessage.modelName}</div>
                                <div className="chat-tuners">
                                    {lastMessage.tuners.map(tuner => (
                                        <span key={tuner} className="tuner-tag">{tuner}</span>
                                    ))}
                                </div>
                                <div className="chat-time">
                                    Last: {new Date(lastMessage.answeredAt ?? lastMessage.askedAt).toLocaleString()}
                                </div>
                            </div>

                            <div className="chat-question">
                                <em>"{lastMessage.question}"</em>
                            </div>

                            <div className="chat-meta">
                                <div><strong>Norma:</strong> {base.norm}</div>
                                <div><strong>Seria:</strong> {base.series}</div>
                                <div><strong>Release:</strong> {base.release}</div>
                            </div>

                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default ChatHistoryPanel;
