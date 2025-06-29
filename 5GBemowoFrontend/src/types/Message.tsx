export interface UsedChunk {
    text: string;
    index: number;
}

export interface HighlightedFragment {
    quote: string;
    occurrence: number;
    chunkIndex: number;
}

export interface Message {
    id: string;
    question: string;
    answer: string;
    modelName: string;
    tuners: string[];
    askedAt: number;
    answeredAt?: number;
    answered: boolean;
    userId: number;
    chatId: string;
    baseId: string;
    release: string;
    series: string;
    norm: string;
    usedContextChunks: UsedChunk[];
    highlighetedFragments: HighlightedFragment[];
}
