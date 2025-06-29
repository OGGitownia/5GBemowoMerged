import { NavigateFunction } from "react-router-dom";

export const logoutUser = async (navigate: NavigateFunction, setUser: (user: null) => void) => {
    const token = localStorage.getItem("token");

    if (!token) {
        console.warn("Brak tokenu w localStorage, nie można zakończyć sesji.");

        setUser(null);

        navigate("/login", { replace: true });
        return;
    }
    try {
        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/users/session/${token}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (response.ok) {
            console.log("Sesja zakończona poprawnie.");


            localStorage.removeItem("token");

            setUser(null);

            navigate("/login", { replace: true });
        } else {
            console.error("Nie udało się zakończyć sesji.");
            alert("Wystąpił problem podczas wylogowywania. Spróbuj ponownie");
        }
    } catch (error) {
        console.error("Błąd podczas usuwania sesji:", error);
        alert("Błąd połączenia z serwerem");
    }
};
