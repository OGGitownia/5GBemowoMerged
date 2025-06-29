import React, { useEffect, useState } from "react";
import "../styles/modals/StartChatModal.css"
import {BaseInfo} from "../types/BaseInfo.tsx";
import "../styles/modals/StartChatModal.css"

interface StartChatModalProps {
    baseInfo: BaseInfo;
    onClose: () => void;
    onStartChat: (model: string, options: string[]) => void;
}


const StartChatModal: React.FC<StartChatModalProps> = ({ baseInfo, onClose, onStartChat }) => {
    const [models, setModels] = useState<string[]>([]);
    const [selectedModel, setSelectedModel] = useState<string | null>(null);
    const [tuners, setTuners] = useState<string[]>([]);
    const [selectedTuners, setSelectedTuners] = useState<string[]>([]);
    const [loadingTuners, setLoadingTuners] = useState(false);

    useEffect(() => {
        fetchModels();
    }, [baseInfo.id]);

    const fetchModels = async () => {
        const response = await fetch(`/api/chat/available-models/${baseInfo.id}`);
        const data = await response.json();
        setModels(data);
    };

    const fetchTuners = async (model: string) => {
        setLoadingTuners(true);
        const response = await fetch(`/api/chat/available-tuners/${baseInfo.id}?model=${model}`);
        const data = await response.json();
        setTuners(data);
        setLoadingTuners(false);
    };

    const handleModelSelect = (model: string) => {
        setSelectedModel(model);
        setSelectedTuners([]);
        fetchTuners(model);
    };

    const toggleTuner = (tuner: string) => {
        setSelectedTuners((prev) =>
            prev.includes(tuner) ? prev.filter((t) => t !== tuner) : [...prev, tuner]
        );
    };


    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <h2>Start New Chat with <span className="highlight">{'baseName'}</span></h2>

                <div className="section">
                    <h4>Select Model</h4>
                    <div className="options-list">
                        {models.map((model) => (
                            <button
                                key={model}
                                className={`model-btn ${selectedModel === model ? "selected" : ""}`}
                                onClick={() => handleModelSelect(model)}
                            >
                                {model}
                            </button>
                        ))}
                    </div>
                </div>

                {selectedModel && (
                    <div className="section">
                        <h4>Answer Tuners</h4>
                        {loadingTuners ? (
                            <p className="loading">Loading tuners...</p>
                        ) : (
                            <div className="checkbox-list">
                                {tuners.map((tuner) => (
                                    <label key={tuner} className={`checkbox-item ${selectedTuners.includes(tuner) ? "selected" : ""}`}>
                                        <input
                                            type="checkbox"
                                            checked={selectedTuners.includes(tuner)}
                                            onChange={() => toggleTuner(tuner)}
                                        />
                                        <span>{tuner}</span>
                                    </label>

                                ))}
                            </div>
                        )}
                    </div>
                )}

                <div className="modal-actions">
                    <button className="cancel-btn" onClick={onClose}>Cancel</button>
                    <button
                        className="start-btn"
                        onClick={() => selectedModel && onStartChat(selectedModel, selectedTuners)}
                        disabled={!selectedModel}
                    >
                        Start Chat
                    </button>
                </div>
            </div>
        </div>
    );
};

export default StartChatModal;
