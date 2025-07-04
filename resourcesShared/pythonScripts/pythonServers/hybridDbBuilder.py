from flask import Flask, request, jsonify
import threading
import os
import signal
import sys
import json
import requests
import faiss
import sqlite3
import numpy as np

sys.stdout.reconfigure(encoding='utf-8')

app = Flask(__name__)

server_name = sys.argv[1] if len(sys.argv) > 1 else "hybridDbBuilder"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5007
SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"


@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    print("[START] process_embedding_request", flush=True)
    try:
        data = request.get_json()
        print(f"[DATA] Odebrano dane: {data}", flush=True)

        input_path = data.get("inputPath")
        output_path = data.get("outputPath")

        if not input_path or not output_path:
            return jsonify({"error": "Brakuje pola 'inputPath' lub 'outputPath'"}), 400

        if not os.path.exists(input_path):
            return jsonify({"error": f"Plik wejściowy nie istnieje: {input_path}"}), 400

        if not os.path.exists(output_path):
            os.makedirs(output_path)

        with open(input_path, "r", encoding="utf-8") as f:
            json_data = json.load(f)

        if isinstance(json_data, dict) and "chunks" in json_data:
            fragments = [
                {
                    "index": chunk.get("index"),
                    "content": chunk.get("content", ""),
                    "embeddedContent": chunk.get("embeddedContent", [])
                }
                for chunk in json_data["chunks"]
            ]
        else:
            return jsonify({"error": "Nieprawidłowa struktura JSON — brak pola 'chunks'"}), 400

        sentences = []
        embeddings = []
        source_indexes = []

        for i, entry in enumerate(fragments):
            if "content" not in entry or "embeddedContent" not in entry:
                return jsonify({"error": f"Błąd w fragmencie {i} — brak 'content' lub 'embeddedContent'"}), 400
            source_indexes.append(entry.get("index", i))
            sentences.append(entry["content"])
            embeddings.append(entry["embeddedContent"])

        embeddings = np.array(embeddings).astype('float32')
        if embeddings.size == 0:
            return jsonify({"error": "Brak embeddingów"}), 400

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
        return jsonify({"message": "Hybrid database created successfully"}), 200

    except Exception as e:
        import traceback
        print("[EXCEPTION]", traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500


@app.route(f"/{server_name}/shutdown", methods=["POST"])
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    response = jsonify({"message": "Shutting down server"})
    threading.Thread(target=shutdown_server).start()
    return response, 200


@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "running"}), 200


def notify_spring_boot():
    print("Notifying Spring Boot...", flush=True)
    try:
        r = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print("Notification response:", r.status_code, r.text, flush=True)
    except Exception as e:
        print("Notification failed:", e, flush=True)


def run_server():
    notify_spring_boot()
    app.run(host="0.0.0.0", port=port)


if __name__ == "__main__":
    run_server()
