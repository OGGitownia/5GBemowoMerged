import React from "react";
import "../../styles/smallComponents/SeriesPanel.css";
import {Series} from "../../types/Series.tsx";



interface SeriesPanelProps {
    series: Series;
    onClick: () => void;
}

export const SeriesPanel: React.FC<SeriesPanelProps> = ({ series, onClick }) => {
    return (
        <div className="series-panel" onClick={onClick}>
            <div className="header">
                <div className="name">{series.name}</div>
            </div>
            <div className="description">{series.description}</div>
        </div>
    );
};


