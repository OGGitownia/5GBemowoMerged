import React from "react";
import "../../styles/smallComponents/NormPanel.css";
import { Norm } from "../../types/Norm.tsx";

interface NormPanelProps {
    norm: Norm;
    onShowBases: () => void;
    onAddBase: () => void;
}

const NormPanel: React.FC<NormPanelProps> = ({ norm, onShowBases, onAddBase }) => {
    const getBackgroundColor = () => {
        if (norm.numberOfBases === 0) return "rgb(42, 42, 42)";
        if (norm.numberOfBases === 1) return "rgba(255, 255, 92, 0.15)";
        return "rgba(92, 255, 92, 0.15)";
    };

    return (
        <div className="norm-panel" style={{ backgroundColor: getBackgroundColor() }}>
            <div className="header">
                <div className="name">
                    {norm.title} - {norm.specNumber}
                </div>
            </div>
            <div className="details">
                <span><strong>Versions:</strong> {norm.versions.join(", ")}</span>
                <span><strong>Latest Version:</strong> {norm.latestVersion}</span>
                <span><strong>Publication Date:</strong> {norm.date}</span>
                <span><strong>Size:</strong> {norm.size}</span>
                <span><strong>Number of Bases:</strong> {norm.numberOfBases}</span>
            </div>

            <div className="button-group">
                <button className="show-bases-btn" onClick={onShowBases}>
                    Show Bases
                </button>
                <button className="add-base-btn" onClick={onAddBase}>
                    Add Base
                </button>
            </div>
        </div>
    );
};

export default NormPanel;