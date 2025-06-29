import { useState } from "react";
import "../styles/views/LoginSignup.css";
import { useNavigate } from "react-router-dom";
import user_icon from "../assets/person.png";
import email_icon from "../assets/email.png";
import password_icon from "../assets/password.png";
import { ValidateData } from "../services/ValidateData";
import { getNewSessionToken } from "../services/authService.tsx";
import { loginWithEmail } from "../services/login.tsx";
import { User } from "../types/User.tsx";

const handleClick = async (
    action: string,
    username: string,
    email: string,
    password: string,
    setAction: React.Dispatch<React.SetStateAction<"Sign Up" | "Login" | "Reset">>,
    navigate: ReturnType<typeof useNavigate>,
    setNameError: (msg: string) => void,
    setEmailError: (msg: string) => void,
    setPasswordError: (msg: string) => void
) => {
    if (action === "Sign Up") {
        let hasError = false;

        if (!ValidateData.isValidName(username)) {
            setNameError("Name must be at least 4 characters and contain letters or digits.");
            hasError = true;
        } else {
            setNameError("");
        }

        if (!ValidateData.isValidEmail(email)) {
            setEmailError("Invalid email address.");
            hasError = true;
        } else {
            setEmailError("");
        }

        if (!ValidateData.isValidPassword(password)) {
            setPasswordError("Password must be at least 8 characters long, include one uppercase letter, one number, and one special character.");
            hasError = true;
        } else {
            setPasswordError("");
        }

        if (hasError) return;

        const url = `${import.meta.env.VITE_API_URL}/api/users/register/email`;
        const data = {
            username: username,
            email: email,
            password: password
        };
        console.log(data);

        try {
            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(data),
            });

            console.log(response.status);

            if (response.ok) {
                const result = await response.json();
                console.log("Rejestracja zakończona sukcesem:", result);

                const user: User = {
                    id: result.id,
                    email: result.email,
                    phoneNumber: result.phoneNumber,
                    username: result.username,
                    password: result.password,
                    avatarPath: result.avatarPath,
                    createdAt: result.createdAt,
                    lastActiveAt: result.lastActiveAt,
                    roles: result.roles,
                    isActive: result.isActive,
                    emailVerified: result.emailVerified,
                    phoneNumberVerified: result.phoneNumberVerified,
                };
                localStorage.setItem("user", JSON.stringify(user));
                const token = await getNewSessionToken(user.id, navigate);
                if (token) {
                    console.log("Token zapisany w localStorage:", token);
                    localStorage.setItem("token", token);
                    navigate("/verifyEmail");
                }
            } else {
                const errorText = await response.text();
                console.error("Błąd podczas rejestracji:", errorText);
            }
        } catch (error) {
            console.error("Błąd podczas wysyłania żądania:", error);
        }
    } else {
        setAction("Sign Up");
    }
};

function LoginSignup() {
    const [action, setAction] = useState<"Sign Up" | "Login" | "Reset">("Sign Up");
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [nameError, setNameError] = useState("");
    const [emailError, setEmailError] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const navigate = useNavigate();

    const handleLoginClick = () => {
        loginWithEmail(email, password, navigate);
    };

    return (
        <div className='containter'>
            <div className="header">
                <div className="text">{action}</div>
                <div className="underline"></div>
            </div>
            <div className="inputs">
                {action === "Reset" ? (
                    <>
                        <p className="info-text">
                            Enter your email address to receive a password reset link.
                        </p>
                        <div className="input">
                            <img src={email_icon} alt="email icon" />
                            <input
                                type="email"
                                placeholder="Email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                    </>
                ) : (
                    <>
                        {action === "Login" ? null : (
                            <>
                                <div className="input">
                                    <img src={user_icon} alt="user icon" />
                                    <input
                                        type="text"
                                        placeholder="Name"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                    />
                                </div>
                                {nameError && <p className="error-text">{nameError}</p>}
                            </>
                        )}
                        <div className="input">
                            <img src={email_icon} alt="email icon" />
                            <input
                                type="email"
                                placeholder="Email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                        {emailError && <p className="error-text">{emailError}</p>}
                        <div className="input">
                            <img src={password_icon} alt="password icon" />
                            <input
                                type="password"
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                        {passwordError && <p className="error-text">{passwordError}</p>}
                    </>
                )}
            </div>

            {action === "Login" && (
                <div className="forgot-password">
                    Lost Password?
                    <span className="click-here" onClick={() => setAction("Reset")}>Click Here!</span>
                </div>
            )}

            <div className="submit-container">
                {action === "Reset" ? (
                    <div className="submit" onClick={() => alert(`Reset link sent to ${email}`)}>
                        Send Reset Link
                    </div>
                ) : (
                    <>
                        <div
                            className={action === "Login" ? "submit gray" : "submit"}
                            onClick={() =>
                                handleClick(
                                    action,
                                    username,
                                    email,
                                    password,
                                    setAction,
                                    navigate,
                                    setNameError,
                                    setEmailError,
                                    setPasswordError
                                )
                            }
                        >
                            Sign Up
                        </div>
                        <div
                            className={action === "Sign Up" ? "submit gray" : "submit"}
                            onClick={() => {
                                if (action === "Login") handleLoginClick();
                                else setAction("Login");
                            }}
                        >
                            Login
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

export default LoginSignup;
