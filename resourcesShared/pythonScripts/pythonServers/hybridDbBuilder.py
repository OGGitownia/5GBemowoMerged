import os
import sys
import json
import signal
import threading
import requests
import sqlite3
import faiss
import numpy as np

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI()

server_name = sys.argv[1] if len(sys.argv) > 1 else "hybridDbBuilder"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5007
SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"


@app.post(f"/{server_name}/process")
async def process_embedding_request(request: Request):
    print("[START] process_embedding_request", flush=True)
    try:
        data = await request.json()
        print(f"[DATA] Odebrano dane: {data}", flush=True)

        input_path = data.get("inputPath")
        output_path = data.get("outputPath")

        if not input_path or not output_path:
            return JSONResponse(content={"error": "Brakuje pola 'inputPath' lub 'outputPath'"}, status_code=400)

        if not os.path.exists(input_path):
            return JSONResponse(content={"error": f"Plik wejściowy nie istnieje: {input_path}"}, status_code=400)

        if not os.path.exists(output_path):
            os.makedirs(output_path)

        with open(input_path, "r", encoding="utf-8") as f:
            json_data = json.load(f)

        if not isinstance(json_data, dict) or "chunks" not in json_data:
            return JSONResponse(content={"error": "Nieprawidłowa struktura JSON — brak pola 'chunks'"}, status_code=400)

        fragments = [
            {
                "index": chunk.get("index"),
                "content": chunk.get("content", ""),
                "embeddedContent": chunk.get("embeddedContent", [])
            }
            for chunk in json_data["chunks"]
        ]

        sentences = []
        embeddings = []
        source_indexes = []

        for i, entry in enumerate(fragments):
            if "content" not in entry or "embeddedContent" not in entry:
                return JSONResponse(content={"error": f"Błąd w fragmencie {i} — brak 'content' lub 'embeddedContent'"}, status_code=400)
            source_indexes.append(entry.get("index", i))
            sentences.append(entry["content"])
            embeddings.append(entry["embeddedContent"])

        embeddings = np.array(embeddings).astype('float32')
        if embeddings.size == 0:
            return JSONResponse(content={"error": "Brak embeddingów"}, status_code=400)

        dimension = embeddings.shape[1]
        print(f"[INFO] Embedding dimension: {dimension}, count: {len(sentences)}", flush=True)

        # FAISS
        index = faiss.IndexFlatL2(dimension)
        index.add(embeddings)
        index_path = os.path.join(output_path, "hybrid_db.index")
        faiss.write_index(index, index_path)
        print(f"[SUCCESS] Zapisano FAISS index: {index_path}", flush=True)

        # SQLite
        db_path = os.path.join(output_path, "hybrid_db.sqlite")
        if os.path.exists(db_path):
            os.remove(db_path)

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE documents (
                id INTEGER PRIMARY KEY,
                sentence TEXT,
                source_index INTEGER
            )
        """)
        for idx, (sentence, source_index) in enumerate(zip(sentences, source_indexes)):
            cursor.execute("INSERT INTO documents (id, sentence, source_index) VALUES (?, ?, ?)", (idx, sentence, source_index))
        conn.commit()
        conn.close()

        print(f"[SUCCESS] Zapisano SQLite: {db_path}", flush=True)
        return JSONResponse(content={"message": "Hybrid database created successfully"}, status_code=200)

    except Exception as e:
        import traceback
        print("[EXCEPTION]", traceback.format_exc(), flush=True)
        return JSONResponse(content={"error": str(e)}, status_code=500)


@app.post(f"/{server_name}/shutdown")
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    threading.Thread(target=shutdown_server).start()
    return JSONResponse(content={"message": "Shutting down server"}, status_code=200)


@app.get("/status")
def status():
    return JSONResponse(content={"status": "running"}, status_code=200)


def notify_spring_boot():
    print("Notifying Spring Boot...", flush=True)
    try:
        r = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print("Notification response:", r.status_code, r.text, flush=True)
    except Exception as e:
        print("Notification failed:", e, flush=True)


if __name__ == "__main__":
    import uvicorn
    notify_spring_boot()
    uvicorn.run(app, host="0.0.0.0", port=port)
