import axios from "axios";

const BASE_URL = `${import.meta.env.VITE_API_URL}/api/bases`;

export const fetchBaseCreatingMethods = async (): Promise<string[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/methods/get-all`);
        console.log("Fetched base creation methods:", response.data);
        return response.data;
    } catch (error) {
        console.error("Error fetching base creation methods:", error);
        throw error;
    }
};
