import axios from "axios";
import {Release} from "../types/Release.tsx";
import {Series} from "../types/Series.tsx";
import {Norm} from "../types/Norm.tsx";


const BASE_URL = `${import.meta.env.VITE_API_URL}/api/norms`;

export const fetchAllReleases = async (): Promise<Release[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/releases`);
        return response.data.map((item: unknown) => mapToRelease(item));
    } catch (error) {
        console.error("Error fetching releases:", error);
        throw error;
    }
};

export const fetchSeriesForRelease = async (releaseId: string): Promise<Series[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/series/${releaseId}`);
        return response.data.map((item: unknown) => mapToSeries(item));
    } catch (error) {
        console.error(`Error fetching series for release ${releaseId}:`, error);
        throw error;
    }
};

export const fetchNormsForReleaseAndSeries = async (releaseId: string, seriesId: string): Promise<Norm[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/norms/${releaseId}/${seriesId}`);

        return response.data.map((item: unknown) => mapToNorm(item));
    } catch (error) {
        console.error(`Error fetching norms for release ${releaseId} and series ${seriesId}:`, error);
        throw error;
    }
};




const isRelease = (data: unknown): data is Release => {
    return (
        typeof data === "object" &&
        data !== null &&
        "releaseId" in data &&
        "name" in data &&
        "status" in data &&
        "startDate" in data &&
        "endDate" in data &&
        "closureDate" in data
    );
};

const mapToRelease = (data: unknown): Release => {
    if (isRelease(data)) {
        return {
            releaseId: data.releaseId,
            name: data.name,
            status: data.status,
            startDate: data.startDate,
            endDate: data.endDate,
            closureDate: data.closureDate
        };
    }
    throw new Error("Invalid Release format");
};


const isSeries = (data: unknown): data is Series => {
    return (
        typeof data === "object" &&
        data !== null &&
        "seriesId" in data &&
        "name" in data &&
        "description" in data
    );
};

const mapToSeries = (data: unknown): Series => {
    if (isSeries(data)) {
        return {
            seriesId: data.seriesId,
            name: data.name,
            description: data.description
        };
    }
    throw new Error("Invalid Series format");
};


const isNorm = (data: unknown): data is Norm => {
    return (
        typeof data === "object" &&
        data !== null &&
        "specNumber" in data &&
        "title" in data &&
        "versions" in data &&
        "latestVersion" in data &&
        "zipUrl" in data &&
        "date" in data &&
        "size" in data &&
        "numberOfBases" in data
    );
};

const mapToNorm = (data: unknown): Norm => {
    if (isNorm(data)) {
        return {
            release: "", series: "",
            specNumber: data.specNumber,
            title: data.title,
            versions: data.versions,
            latestVersion: data.latestVersion,
            zipUrl: data.zipUrl,
            date: data.date,
            size: data.size,
            numberOfBases: data.numberOfBases
        };
    }
    throw new Error("Invalid Norm format");
};





