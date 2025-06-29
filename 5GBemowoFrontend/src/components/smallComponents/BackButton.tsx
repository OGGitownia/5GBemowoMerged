import React from "react";
import "../../styles/smallComponents/BackButton.css";
import { useNavigate } from "react-router-dom";

const BackButton: React.FC = () => {
    const navigate = useNavigate();

    return (
        <button className="back-button" onClick={() => navigate(-1)}>
            <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
            >
                <polyline points="15 18 9 12 15 6" />
            </svg>
            Back
        </button>
    );
};

export default BackButton;
