import { useNavigate } from "react-router-dom";
import ProfileIcon from '../../assets/profile_icon.svg?react';
import AboutIcon from '../../assets/about_us_icon.svg?react';
import LogoutIcon from '../../assets/logout.svg?react';
import "../../styles/smallComponents/RightIcons.css";
import {logoutUser} from "../../services/logout.tsx";
import { useApp } from "../../services/AppContext.tsx";




export default function RightIcons() {
    const { user, setUser } = useApp();
    const navigate = useNavigate();

    console.log(user);

    return (
        <div className="right-icons">
            <button type="button" className="icon-btn-with-label" onClick={() => navigate("/profile")}>
                <ProfileIcon className="custom-icon" />
                <span className="icon-label">Profil</span>
            </button>

            <button type="button" className="icon-btn-with-label" onClick={() => navigate("/aboutUs")}>
                <AboutIcon className="custom-icon" />
                <span className="icon-label">About Us</span>
            </button>

            <button
                type="button"
                className="icon-btn-with-label"
                onClick={() => logoutUser(navigate, setUser)}
                title="Wyloguj">
                <LogoutIcon className="custom-icon" />
                <span className="icon-label">Logout</span>
            </button>
        </div>
    );
}
