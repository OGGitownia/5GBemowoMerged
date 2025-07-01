import os
import json
import torch
import psutil
import uvicorn
from fastapi import FastAPI, Request
from sentence_transformers import SentenceTransformer
import sys
import requests

print("=== SCRIPT STARTED ===")

try:
    from pynvml import nvmlInit, nvmlDeviceGetHandleByIndex, nvmlDeviceGetMemoryInfo
    nvmlInit()
    GPU_AVAILABLE = torch.cuda.is_available()
    print("GPU AVAILABLE:", GPU_AVAILABLE)
except Exception as e:
    GPU_AVAILABLE = False
    print("GPU CHECK FAILED:", str(e))

app = FastAPI()
device = "cuda" if GPU_AVAILABLE else "cpu"
print("Selected device:", device)

MODEL_NAME = "BAAI/bge-base-en"
print("Loading model:", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, device=device)
print("Model loaded.")

@app.post("/embeddingMaster/process")
async def embed_chunks(request: Request):
    print("Received request")
    try:
        body = await request.json()
        input_file = body.get("inputFile")
        output_file = body.get("outputFile")

        if not input_file or not output_file:
            raise ValueError("Missing inputFile or outputFile")

        print(f"Reading input file: {input_file}")
        with open(input_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        chunks = data.get("chunks", [])
        print(f"Chunks to process: {len(chunks)}")
        texts = [chunk["cleanContent"] for chunk in chunks]

        embeddings = []
        batch_size = 64
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            print(f"Processing batch {i // batch_size + 1} with {len(batch)} elements")
            monitor_resources(i)
            batch_embeds = model.encode(batch, convert_to_numpy=True)
            embeddings.extend(batch_embeds.tolist())

        print("Embedding complete. Adding results to chunks")
        for chunk, embedding in zip(chunks, embeddings):
            chunk["embeddedContent"] = embedding

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump({"chunks": chunks}, f, indent=2)

        print(f"Embedding written to: {output_file}")
        return {"status": "success", "chunksProcessed": len(chunks)}

    except Exception as e:
        print("ERROR:", e)
        return {"error": str(e)}

def monitor_resources(batch_idx):
    ram = psutil.virtual_memory()
    print(f"[Batch {batch_idx}] RAM used: {ram.percent:.1f}%")
    if GPU_AVAILABLE:
        try:
            handle = nvmlDeviceGetHandleByIndex(0)
            mem = nvmlDeviceGetMemoryInfo(handle)
            print(f"[Batch {batch_idx}] GPU used: {mem.used / 1024**2:.1f}MB")
        except Exception as e:
            print("GPU monitoring failed:", e)

@app.get("/")
def ping():
    print("Ping received")
    return {"status": "ok"}

if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "embeddingMaster"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 8011

    print("=== BOOT CONFIG ===")
    print(f"Server name: {name}")
    print(f"Port: {port}")
    print("===================")

    def notify_backend_ready():
        try:
            url = f"http://localhost:8080/{name}/server-ready"
            print("Notifying backend at:", url)
            r = requests.post(url, json={"port": port})
            print("Backend notified:", r.status_code, r.text)
        except Exception as e:
            print("Backend notify error:", e)

    notify_backend_ready()
    print("Starting server...")
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
