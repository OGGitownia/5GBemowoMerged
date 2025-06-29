export interface BaseInfo {
    id: number;
    sourceUrl: string;
    status: string;
    statusMessage: string | null;
    createdWthMethod: string;
    maxContextWindow: number;
    multiSearchAllowed: boolean;
    release: string;
    series: string;
    norm: string;
}
