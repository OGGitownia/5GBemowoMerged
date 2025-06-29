import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/views/Verification.css";
import { useApp } from "../services/AppContext.tsx";


export default function Verification() {
    const { user } = useApp();

    console.log(user)
    const [token, setToken] = useState<string>("");
    const [message, setMessage] = useState<string | null>(null);
    const navigate = useNavigate();

    const handleSubmit = async () => {
        try {
            const response = await fetch(
                `${import.meta.env.VITE_API_URL}/api/users/verify/email?token=${encodeURIComponent(token)}`,
                {
                    method: "GET",
                    headers: { "Content-Type": "application/json" },
                }
            );

            if (response.ok) {
                const data = await response.json();
                console.log("Weryfikacja udana:", data);
                navigate("/select");
            } else {
                const contentType = response.headers.get("Content-Type");
                if (contentType && contentType.includes("application/json")) {
                    const errorData = await response.json();
                    console.error("Błąd weryfikacji:", errorData.error);
                    setMessage(errorData.error);
                } else {
                    const errorText = await response.text();
                    console.error("Błąd HTML:", errorText);
                    setMessage("Wystąpił błąd serwera.");
                }
            }
        } catch (error) {
            console.error("Błąd weryfikacji:", error);
            setMessage("Wystąpił błąd podczas weryfikacji.");
        }
    };



    return (
        <div className="container">
            <div className="header">
                <div className="text">Verification</div>
                <div className="underline"/>
            </div>

            <div className="inputs">
                <div className="email-info">
                     <span className="user-email">{user?.email || "No email found"}</span>
                </div>
                <div className="input field-container">
                    <input
                        className="field-input"
                        type="text"
                        placeholder="Enter verification token"
                        value={token}
                        onChange={(e) => setToken(e.target.value)}
                    />
                </div>
            </div>

            <div className="submit-container">
                <button className="submit" onClick={handleSubmit}>
                    Submit
                </button>
            </div>

            {message && <div className="message">{message}</div>}
        </div>
    );
}
