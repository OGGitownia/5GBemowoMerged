import React, { useEffect, useState } from "react";
import { BaseInfo } from "../../types/BaseInfo.tsx";
import { fetchBases } from "../../services/fetchBases.tsx";
import BaseInfoCard from "../smallComponents/BaseInfoCard.tsx";
import "../../styles/bigComponents/AllBasesComponent.css";

const AllBasesComponent: React.FC = () => {
    const [bases, setBases] = useState<BaseInfo[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadBases = async () => {
            const data = await fetchBases();
            setBases(data);
            setLoading(false);
        };

        loadBases();
    }, []);

    if (loading) return <p className="loading-text">Loading bases...</p>;

    return (
        <div className="all-bases-container">
            <h2 className="all-bases-title">Base List</h2>
            <div className="base-cards-wrapper">
                {bases.map((base) => {
                    console.log("Base:", base); 
                    return <BaseInfoCard key={base.id} base={base} />;
                })}
            </div>

        </div>
    );
};

export default AllBasesComponent;
