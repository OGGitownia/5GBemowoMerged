import { useEffect, useState } from "react";
import {
    fetchAllReleases,
    fetchNormsForReleaseAndSeries,
    fetchSeriesForRelease
} from "../../services/NormDataServices.tsx";
import {Release} from "../../types/Release.tsx";
import {ReleasePanel} from "../smallComponents/ReleasesPanel.tsx";
import {Series} from "../../types/Series.tsx";
import {SeriesPanel} from "../smallComponents/SeriesPanel.tsx";
import {Norm} from "../../types/Norm.tsx";
import NormPanel from "../smallComponents/NormPanel.tsx";
import {useNavigate} from "react-router-dom";
import {useApp} from "../../services/AppContext.tsx";


export default function TreeComponent() {
    const navigate = useNavigate();
    const [releases, setReleases] = useState<Release[]>([]);
    const [series, setSeries] = useState<Series[]>([]);
    const [norms, setNorms] = useState<Norm[]>([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);




    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await fetchAllReleases();
                setReleases(data);
            } catch (err) {
                console.error(err);
                setError("Failed to load releases");
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    const { selectedChatInfo, setSelectedChatInfo } = useApp();

    const handleReleaseClick = async (release: Release) => {
        setLoading(true);
        try {
            const data = await fetchSeriesForRelease(release.releaseId);
            setSeries(data);


            setSelectedChatInfo(prev => ({
                ...prev,
                chatRel: release
            }));
        } catch (err) {
            console.error("Błąd przy pobieraniu serii:", err);
            setError("Failed to load series");
        } finally {
            setLoading(false);
        }
    };


    const handleSeriesClick = async (series: Series) => {
        setLoading(true);
        try {
            const release = selectedChatInfo.chatRel;
            if (!release) {
                throw new Error("Nie wybrano release przed kliknięciem serii.");
            }

            const data = await fetchNormsForReleaseAndSeries(release.releaseId, series.seriesId);
            setNorms(data);

            setSelectedChatInfo(prev => ({
                ...prev,
                chatSeries: series
            }));
        } catch (err) {
            console.error("Błąd przy pobieraniu norm:", err);
            setError("Failed to load norms");
        } finally {
            setLoading(false);
        }
    };



    const handleBackClick = () => {
        if (selectedChatInfo.chatNorm) {
            setSelectedChatInfo(prev => ({
                ...prev,
                chatNorm: null
            }));
        } else if (selectedChatInfo.chatSeries) {
            setSelectedChatInfo(prev => ({
                ...prev,
                chatSeries: null,
                chatNorm: null
            }));
            setNorms([]);
        } else if (selectedChatInfo.chatRel) {
            setSelectedChatInfo(prev => ({
                ...prev,
                chatRel: null,
                chatSeries: null,
                chatNorm: null
            }));
            setSeries([]);
        }
    };

    if (loading) return <div>Loading releases...</div>;
    if (error) return <div>Error: {error}</div>;

    const { chatRel, chatSeries, chatNorm } = selectedChatInfo;

    return (
        <div className="tree-container">
            {chatNorm ? (
                <div className="bases-view">
                    <button className="backTree-button" onClick={handleBackClick}>
                        ← Back to Norms
                    </button>
                    <h3 className="Title-selection">Bases for {chatNorm.title}</h3>
                    <div className="bases-list">
                    </div>
                </div>
            ) : chatSeries ? (
                <div className="norms-view">
                    <div className="back-title-container">
                        <button className="backTree-button" onClick={handleBackClick}>
                            ← Back to Series
                        </button>
                        <h3 className="Title-selection">Norms for {chatSeries.name}</h3>
                    </div>
                    <div className="norms-list">
                        {norms.map((norm) => (
                            <NormPanel
                                key={norm.specNumber}
                                norm={norm}
                                onShowBases={() => {
                                    setSelectedChatInfo(prev => ({
                                        ...prev,
                                        chatNorm: norm
                                    }));
                                }}
                                onAddBase={() => {
                                    if (chatRel && chatSeries) {
                                        setSelectedChatInfo(prev => ({
                                            ...prev,
                                            chatNorm: norm
                                        }));
                                        navigate("/add");
                                    } else {
                                        console.error("Brakuje release lub series!");
                                    }
                                }}
                            />

                        ))}
                    </div>
                </div>
            ) : chatRel ? (
                <div className="series-view">
                    <div className="back-title-container">
                        <button className="backTree-button" onClick={handleBackClick}>
                            ← Back to Releases
                        </button>
                        <h3 className="Title-selection">Series for {chatRel.name}</h3>
                    </div>

                    <div className="series-list">
                        {series.map((series) => (
                            <SeriesPanel
                                key={series.seriesId}
                                series={series}
                                onClick={() => handleSeriesClick(series)}
                            />
                        ))}
                    </div>
                </div>
            ) : (
                <div className="releases-list">
                    {releases.map((release) => (
                        <ReleasePanel
                            key={release.releaseId}
                            release={release}
                            onClick={() => handleReleaseClick(release)}
                        />
                    ))}
                </div>
            )}
        </div>
    );

}
