import { NavigateFunction } from "react-router-dom";


export const fetchUserSession = async (navigate: NavigateFunction) => {
    const token = localStorage.getItem("token");
    console.log("fetch user session", token);

    if (!token) {
        console.error("Brak tokenu, użytkownik niezalogowany");
        navigate("/login")
        return null;
    }

    try {
        const res = await fetch(`${import.meta.env.VITE_API_URL}/api/users/session/${token}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json"
            }

        });

        if (res.status === 200) {
            const user = await res.json();
            console.log("Uzyskano token sesji:", user);
            localStorage.setItem("user", JSON.stringify(user));
            return user;
        } else if (res.status === 401) {
            console.warn("Sesja wygasła lub brak autoryzacji");
            localStorage.removeItem("token");
            navigate("/login");
            return null;
        } else if (res.status === 403) {
            console.warn("Użytkownik nieweryfikowany");
            const user = await res.json();
            localStorage.setItem("user", JSON.stringify(user));
            navigate("/verify-email");
            return null;
        } else {
            console.error("Nieoczekiwany błąd");
            return null;
        }
    } catch (error) {
        console.error("Błąd podczas łączenia się z serwerem:", error);
        return null;
    }
};




export const getNewSessionToken = async (userId: number, navigate: NavigateFunction) => {
    const url = `${import.meta.env.VITE_API_URL}/api/users/session/new/${userId}`;

    try {
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (res.status === 200) {
            const data = await res.json();
            console.log("Nowy token sesji uzyskany:", data.token);

            localStorage.setItem("token", data.token);

            return data.token;
        } else if (res.status === 401) {
            console.warn("Nieautoryzowany dostęp - ponowne logowanie");
            localStorage.removeItem("token");
            navigate("/login");
            return null;
        } else if (res.status === 403) {
            console.warn("Użytkownik nieweryfikowany, wymagana weryfikacja email");
            navigate("/verify-email");
            return null;
        } else {
            console.error("Nieoczekiwany błąd:", res.status);
            return null;
        }
    } catch (error) {
        console.error("Błąd podczas łączenia się z serwerem:", error);
        return null;
    }
};
