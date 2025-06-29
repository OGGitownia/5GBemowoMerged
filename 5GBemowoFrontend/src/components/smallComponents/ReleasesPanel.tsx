import React from 'react';
import "../../styles/smallComponents/ReleasesPanel.css";
import {Release} from "../../types/Release.tsx";


interface ReleasePanelProps {
    release: Release;
    onClick: () => void;
}

export const ReleasePanel: React.FC<ReleasePanelProps> = ({ release, onClick }) => {


    return (
        <div className="release-panel" onClick={onClick}>
            <div className="header">
                <div className="name">
                    {release.name} - {release.status}
                </div>
            </div>
            <div className="dates">
                <div className="date-box">
                    <span>Start:</span> {release.startDate}
                </div>
                <div className="date-box">
                    <span>End:</span> {release.endDate}
                </div>
                <div className="date-box">
                    <span>Closure:</span> {release.closureDate}
                </div>
            </div>
        </div>
    );
};
