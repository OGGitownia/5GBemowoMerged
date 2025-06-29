import React, {JSX, useEffect, useRef, useState} from "react";
import "../styles/modals/MessageModal.css";
import { Message } from "../types/Message";
import axios from "axios";
import parse, { DOMNode, Text } from "html-react-parser";


interface MessageModalProps {
    message: Message;
    onClose: () => void;
}

interface Chunk {
    index: number;
    title: string;
    content: string;
    cleanContent: string;
    level: number;
    mergedTitle: string;
}

const createTransform = (baseId: string) => (node: DOMNode): JSX.Element | string | null => {
    if (node.type === "text") {
        const text = (node as Text).data;
        const regex = /photo_(\d+)\.(png|emf)/gi;
        const parts = [];
        let lastIndex = 0;
        let match;

        while ((match = regex.exec(text)) !== null) {
            const [fullMatch, photoId, ext] = match;
            const start = match.index;
            const end = regex.lastIndex;

            if (start > lastIndex) {
                parts.push(text.slice(lastIndex, start));
            }

            const imageUrl = `/api/photos/${baseId}/${fullMatch}`;
            parts.push(
                <img
                    key={`${photoId}-${ext}-${start}`}
                    src={imageUrl}
                    alt={`photo ${photoId}`}
                    className="embedded-photo"
                />
            );

            lastIndex = end;
        }

        if (lastIndex < text.length) {
            parts.push(text.slice(lastIndex));
        }

        return parts.length > 0 ? <>{parts}</> : text;
    }

    return null;
};



const MessageModal: React.FC<MessageModalProps> = ({ message, onClose }) => {
    const [activeView, setActiveView] = useState<'chunks' | 'norm'>('chunks');
    const [normChunks, setNormChunks] = useState<Chunk[]>([]);

    const chunkRefs = useRef<Map<number, HTMLDivElement>>(new Map());
    useRef<(HTMLDivElement | null)[]>([]);
    useEffect(() => {
        axios.get(`${import.meta.env.VITE_API_URL}/api/chat/chunks/${message.baseId}`)
            .then(response => {
                if (!response.data || !Array.isArray(response.data.chunks)) {
                    console.error("Błędna odpowiedź z serwera. Oczekiwano `chunks` jako tablicy.", response.data);
                    return;
                }
                setNormChunks(response.data.chunks);
            })
            .catch(error => {
                console.error(`Błąd podczas pobierania normy dla baseId=${message.baseId}:`, error);
            });
    }, [message.baseId]);

    const escapeRegExp = (str: string): string =>
        str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    const highlightText = (text: string, highlights: string[]) => {
        if (highlights.length === 0) return text;

        const sorted = [...highlights].sort((a, b) => b.length - a.length);
        let highlightedText = text;

        sorted.forEach(fragment => {
            const escaped = escapeRegExp(fragment);
            try {
                highlightedText = highlightedText.replace(
                    new RegExp(escaped, 'g'),
                    match => `<span class="highlight">${match}</span>`
                );
            } catch (e) {
                console.warn("Nie udało się zastosować highlight:", fragment, e);
            }
        });

        return highlightedText;
    };

    const scrollToChunk = (index: number) => {
        const el = chunkRefs.current.get(index);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    };

    return (
        <div className="message-modal-overlay">
            <div className="message-modal-content">
                <div className="modal-header">
                    <button className={activeView === 'chunks' ? 'active' : ''} onClick={() => setActiveView('chunks')}>
                        Only Chunks
                    </button>
                    <button className={activeView === 'norm' ? 'active' : ''} onClick={() => setActiveView('norm')}>
                        Entire Norm
                    </button>
                    <button className="close-button" onClick={onClose}>Close</button>
                </div>

                {activeView === "chunks" && (
                    <div className="modal-body">
                        {message.usedContextChunks.map((chunk, index) => {
                            const relatedFragments = message.highlighetedFragments
                                .filter(frag => frag.chunkIndex === chunk.index)
                                .map(frag => frag.quote);

                            const html = highlightText(chunk.text, relatedFragments);

                            return (
                                <div
                                    key={index}
                                    className="chunk-block yellow-bg"
                                    ref={el => void (el && chunkRefs.current.set(chunk.index, el))}
                                >
                                    <h3>Chunk #{chunk.index}</h3>
                                    <div>{parse(html, { replace: createTransform(message.baseId) })}</div>

                                </div>
                            );
                        })}
                    </div>
                )}


                {activeView === "norm" && (
                    <div className="norm-layout">
                        <div className="modal-sidebar">
                            <h4>Chunks</h4>
                            {message.usedContextChunks.map((chunk) => (
                                <button key={chunk.index} onClick={() => scrollToChunk(chunk.index)}>
                                    Chunk {chunk.index}
                                </button>

                            ))}

                            <h4>Highlights</h4>
                            {message.highlighetedFragments.map((frag, i) => (
                                <button
                                    key={i}
                                    className="highlight-btn"
                                    onClick={() => scrollToChunk(frag.chunkIndex)}
                                >
                                    {frag.quote.slice(0, 30)}...
                                </button>
                            ))}
                        </div>

                        <div className="modal-main">
                            {normChunks.map((chunk) => {
                                const isUsed = message.usedContextChunks.some(used => used.index === chunk.index);
                                const relatedFragments = message.highlighetedFragments
                                    .filter(frag => frag.chunkIndex === chunk.index)
                                    .map(frag => frag.quote);
                                const html = highlightText(chunk.cleanContent, relatedFragments);

                                return (
                                    <div
                                        key={chunk.index}
                                        className={`chunk-block ${isUsed ? 'used' : ''}`}
                                        ref={el => void (el && chunkRefs.current.set(chunk.index, el))}
                                    >
                                        <h3>{chunk.title}</h3>
                                        <div>{parse(html, { replace: createTransform(message.baseId) })}</div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </div>
        </div>

    );
};

export default MessageModal;
