import React, { useState } from "react";
import "../../styles/smallComponents/BaseInfoCard.css";
import { BaseInfo } from "../../types/BaseInfo.tsx";
import StartChatModal from "../../modals/StartChatModal.tsx";
import { useNavigate } from "react-router-dom";

interface BaseCardProps {
    base: BaseInfo;
}

const BaseInfoCard: React.FC<BaseCardProps> = ({ base }) => {
    const [showModal, setShowModal] = useState(false);
    const navigate = useNavigate();

    const handleCardClick = () => {
        setShowModal(true);
    };

    const handleCloseModal = () => {
        setShowModal(false);
    };

    return (
        <>
            <div className="base-card" onClick={handleCardClick}>
                <div className="base-info-grid">
                    <div className="base-info-column">
                        <p><strong>Status:</strong> {base.status}</p>
                        {base.statusMessage && <p><strong>Status message:</strong> {base.statusMessage}</p>}
                        <p><strong>Method:</strong> {base.createdWthMethod}</p>
                        <p><strong>Max context:</strong> {base.maxContextWindow}</p>
                        <p><strong>Multi-search:</strong> {base.multiSearchAllowed ? "Yes" : "No"}</p>
                    </div>
                    <div className="base-info-column">
                        <p><strong>Norma:</strong> {base.norm}</p>
                        <p><strong>Seria:</strong> {base.series}</p>
                        <p><strong>Release:</strong> {base.release}</p>
                    </div>
                </div>
            </div>

            {showModal && (
                <StartChatModal
                    baseInfo={base}
                    onClose={handleCloseModal}
                    onStartChat={(model, options) => {
                        console.log("Starting chat with", model, options, base);
                        handleCloseModal();
                        navigate("/chat", {
                            state: {
                                model,
                                tuners: options,
                                base,
                                chatId: -1
                            }
                        });
                    }}
                />
            )}
        </>
    );
};

export default BaseInfoCard;
