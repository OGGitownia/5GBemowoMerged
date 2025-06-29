import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/bigComponents/AddNewBase.css";
import { Norm } from "../types/Norm.tsx";
import { fetchBaseCreatingMethods } from "../services/fetchBaseCreatingMethods.tsx";
import { createBase } from "../services/createBase.tsx";
import { useApp } from "../services/AppContext.tsx";
import { Spinner } from "reactstrap";
import { useBaseStatusSocket } from "../services/useBaseStatusSocket.tsx";
import BackButton from "../components/smallComponents/BackButton.tsx";


const AddNewBase: React.FC = () => {
    const navigate = useNavigate();
    const { selectedChatInfo, user } = useApp();

    const [norm, setNorm] = useState<Norm | null>(null);
    const [methods, setMethods] = useState<string[]>([]);
    const [selectedMethod, setSelectedMethod] = useState<string | null>(null);
    const [isConfirmed, setIsConfirmed] = useState(false);
    const [currentStatus, setCurrentStatus] = useState<string>("Waiting for initial status...");
    const [statusHistory, setStatusHistory] = useState<string[]>([]);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [baseId, setBaseId] = useState<string | null>(null);

    useEffect(() => {
        if (selectedChatInfo.chatNorm) {
            setNorm(selectedChatInfo.chatNorm);
        } else {
            console.error("Brak normy w kontekście! Przekierowanie do głównego widoku.");
            navigate("/select");
        }
    }, [selectedChatInfo.chatNorm, navigate]);



    useEffect(() => {
        const fetchMethods = async () => {
            try {
                const availableMethods = await fetchBaseCreatingMethods();
                setMethods(availableMethods);
            } catch (error) {
                console.error("Error fetching methods:", error);
            }
        };

        fetchMethods();
    }, []);


    const updateStatus = useCallback((newStatus: string) => {
        setStatusHistory((prev) => [newStatus, ...prev]);
        setCurrentStatus(newStatus);
    }, []);


    useBaseStatusSocket(baseId, updateStatus);


    const handleSelectChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedMethod(event.target.value);
        setErrorMessage(null);
    };


    const handleCreateBase = () => {
        const { chatRel, chatSeries, chatNorm } = selectedChatInfo;

        if (!chatRel || !chatSeries || !chatNorm) {
            console.error("Brakuje release, series lub normy w kontekście.");
            setErrorMessage("Missing release, series, or norm information.");
            return;
        }

        if (!selectedMethod) {
            console.error("Metoda tworzenia bazy nie została wybrana.");
            setErrorMessage("You must select a method before confirming!");
            return;
        }

        if (!user?.id) {
            console.error("Brakuje użytkownika w kontekście.");
            setErrorMessage("User not logged in.");
            return;
        }

        setIsConfirmed(true);
        setCurrentStatus("Waiting for the first status...");
        setIsLoading(true);

        createBase(
            chatNorm.zipUrl,
            selectedMethod,
            32000,
            true,
            chatRel.name,
            chatSeries.name,
            chatNorm.specNumber
        ).then((returnedBaseId) => {
                console.log("Base creation initiated. ID:", returnedBaseId);
                setBaseId(returnedBaseId);
            })
            .catch((error) => {
                console.error("Error during base creation:", error);
                setErrorMessage("Failed to create base. Please try again.");
                setIsLoading(false);
            });
    };



    return norm ? (
        <div className="norm-details-container">
            <div className="info-header">
                <h2>You are creating a new base for LLM</h2>

                <p>
                    <strong>Release:</strong> {selectedChatInfo.chatRel?.name ?? <em>Not selected</em>}
                </p>
                <p>
                    <strong>Series:</strong> {selectedChatInfo.chatSeries?.name ?? <em>Not selected</em>}
                </p>
                <p>
                    <strong>Norm:</strong> {norm.title} (<strong>{norm.specNumber}</strong>)
                </p>
            </div>


            {!isConfirmed ? (
                <div className="selection-area">
                    <select className="method-select" onChange={handleSelectChange} disabled={isConfirmed}>
                        <option value="">Select Method</option>
                        {methods.map((method, index) => (
                            <option key={index} value={method}>
                                {method}
                            </option>
                        ))}
                    </select>

                    <button
                        className="confirm-button"
                        onClick={handleCreateBase}
                        disabled={isLoading}
                    >
                        Confirm / Create
                    </button>

                    {isLoading && (
                        <div className="loading-spinner">
                            <Spinner />
                            <p>Your base is being created...</p>
                        </div>
                    )}

                    {errorMessage && <p className="error-message">{errorMessage}</p>}
                </div>
            ) : (
                <div className="confirmation-area">
                    <p><strong>Selected method:</strong> {selectedMethod}</p>
                </div>
            )}

            <div className="status-container">
                <h4 className="current-status">Current Status:</h4>
                <p className="current-status">{currentStatus}</p>

                <h4 className="current-status">Previous Statuses:</h4>
                <ul className="status-history">
                    {statusHistory.map((status, index) => (
                        <li key={index}>{status}</li>
                    ))}
                </ul>
            </div>
            <BackButton/>
        </div>
    ) : (
        <div className="loading-message">Loading norm details...</div>
    );
};

export default AddNewBase;
