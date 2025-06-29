import axios from "axios";

export const createBase = async (
    sourceUrl: string,
    selectedMethod: string,
    maxContextWindow: number,
    multiSearchAllowed: boolean,
    release: string,
    series: string,
    norm: string
): Promise<string> => {
    const payload = {
        sourceUrl,
        selectedMethod,
        maxContextWindow,
        multiSearchAllowed,
        release,
        series,
        norm
    };

    const response = await axios.post(`${import.meta.env.VITE_API_URL}/api/bases/create`, payload, {
        headers: { "Content-Type": "application/json" }
    });

    return response.data.baseId;
};
