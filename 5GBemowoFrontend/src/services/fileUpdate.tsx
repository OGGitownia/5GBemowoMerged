export const fileUpdate = async (
    file: File,
    setSelectedFile: (file: File | null) => void
) => {
    const formData = new FormData();
    formData.append("avatar", file);

    try {
        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/users/avatar`, {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            const result = await response.json();
            console.log("Zdjęcie zaktualizowane:", result);
            setSelectedFile(null);
        } else {
            const errorText = await response.text();
            throw new Error("Błąd aktualizacji: " + errorText);
        }
    } catch (err) {
        console.error("Błąd wysyłania zdjęcia:", err);
    }
};
