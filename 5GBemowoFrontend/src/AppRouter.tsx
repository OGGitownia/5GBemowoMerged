import { Routes, Route, useNavigate, Navigate } from "react-router-dom";
import { useEffect, useState } from "react";
import LoginSignup from "./views/LoginSignup.tsx";
import MainView from "./views/MainView.tsx";
import AddNewBase from "./views/AddNewBase.tsx";
import ChatView from "./views/ChatView.tsx";
import VerifyEmailView from "./views/Verification.tsx";
import { fetchUserSession } from "./services/authService.tsx";
import { useApp } from "./services/AppContext.tsx";
import AboutUsView from "./views/AboutUsView.tsx";
import ProfileView from "./views/ProfileView.tsx";


function AppRouter() {
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const { user, setUser } = useApp();

    useEffect(() => {
        const checkSession = async () => {
            const userData = await fetchUserSession(navigate);
            if (userData) {
                setUser(userData);
            }
            setLoading(false);
        };
        checkSession();
    }, [navigate, setUser]);

    if (loading) {
        return <div>Ładowanie aplikacji...</div>;
    }

    return (
        <Routes>
            {!user ? (
                <>
                    <Route path="/" element={<LoginSignup />} />
                    <Route path="*" element={<LoginSignup />} />
                </>
            ) : !user.emailVerified ? (
                <>
                    <Route path="/verifyEmail" element={<VerifyEmailView />} />
                    <Route path="*" element={<Navigate to="/verifyEmail" />} />
                </>
            ) : (
                <>
                    <Route path="/select" element={<MainView />} />
                    <Route path="/add" element={<AddNewBase />} />
                    <Route path="/chat" element={<ChatView />} />
                    <Route path="/aboutUs" element={<AboutUsView />} />
                    <Route path="/profile" element={<ProfileView />} />
                    <Route path="*" element={<Navigate to="/select" />} />
                </>
            )}
        </Routes>
    );

}

export default AppRouter;
