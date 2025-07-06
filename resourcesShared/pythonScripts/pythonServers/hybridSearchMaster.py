import os
import sys
import faiss
import json
import signal
import sqlite3
import traceback
import numpy as np
import requests
import psutil
import torch
import threading
from fastapi.responses import JSONResponse



from fastapi import FastAPI, Request
from sentence_transformers import SentenceTransformer

try:
    from pynvml import nvmlInit, nvmlDeviceGetHandleByIndex, nvmlDeviceGetMemoryInfo
    nvmlInit()
    GPU_AVAILABLE = torch.cuda.is_available()
    print("GPU AVAILABLE:", GPU_AVAILABLE)
except Exception as e:
    GPU_AVAILABLE = False
    print("GPU CHECK FAILED:", str(e))

device = "cuda" if GPU_AVAILABLE else "cpu"
print("Selected device:", device)

MODEL_NAME = "BAAI/bge-base-en"
print("Loading model:", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, device=device)
print("Model loaded")

app = FastAPI()

server_name = sys.argv[1] if len(sys.argv) > 1 else "hybridSearchMaster"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5005

loaded_faiss = {}
loaded_documents = {}

def notify_backend_ready():
    try:
        url = f"http://localhost:8080/{server_name}/server-ready"
        print("Notifying backend at:", url)
        r = requests.post(url, json={"port": port})
        print("Backend notified:", r.status_code, r.text)
    except Exception as e:
        print("Backend notify error:", e)

def load_hybrid_database(base_path):
    if base_path in loaded_faiss:
        return loaded_faiss[base_path], loaded_documents[base_path]

    index_path = os.path.join(base_path, "hybrid_db.index")
    sqlite_path = os.path.join(base_path, "hybrid_db.sqlite")

    if not os.path.exists(index_path) or not os.path.exists(sqlite_path):
        raise FileNotFoundError("Hybrid database files not found.")

    faiss_index = faiss.read_index(index_path)

    conn = sqlite3.connect(sqlite_path)
    cursor = conn.cursor()
    cursor.execute("SELECT id, sentence, source_index FROM documents")
    documents = {
        row[0]: {"sentence": row[1], "source_index": row[2]}
        for row in cursor.fetchall()
    }
    conn.close()

    loaded_faiss[base_path] = faiss_index
    loaded_documents[base_path] = documents

    return faiss_index, documents

def embed_query(text, dimension):
    if not text or not isinstance(text, str):
        raise ValueError("Invalid input text.")
    embedding = model.encode(text, convert_to_numpy=True)
    embedding = np.array(embedding, dtype=np.float32)
    if embedding.shape[0] != dimension:
        raise ValueError(f"Expected embedding dimension {dimension}, got {embedding.shape[0]}")
    return embedding

@app.post(f"/{server_name}/shutdown")
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    threading.Thread(target=shutdown_server).start()
    return JSONResponse(content={"message": "Shutting down server"}, status_code=200)


@app.post(f"/{server_name}/process")
async def process_query(request: Request):
    try:
        data = await request.json()
        query = data.get("query")
        base_path = data.get("basePath")

        if not query or not base_path:
            return {"error": "Missing 'query' or 'basePath' in request."}

        print(f"Processing query: {query} @ {base_path}")

        faiss_index, documents = load_hybrid_database(base_path)
        query_embedding = embed_query(query, faiss_index.d)
        _, indices = faiss_index.search(np.array([query_embedding]), k=5)

        results = []
        for idx in indices[0]:
            if idx in documents:
                doc = documents[idx]
                results.append({
                    "sentence": doc["sentence"],
                    "sourceIndex": doc["source_index"]
                })

        return {"results": results}

    except Exception as e:
        print(traceback.format_exc())
        return {"error": str(e)}

@app.get("/")
def ping():
    return {"status": "ok"}

if __name__ == "__main__":
    print("=== BOOT CONFIG ===")
    print(f"Server name: {server_name}")
    print(f"Port: {port}")
    print("===================")

    notify_backend_ready()
    import uvicorn
    print("Starting server...")
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
