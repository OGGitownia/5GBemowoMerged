from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import threading
import os
import signal
import sys
import json
import requests
import time

sys.stdout.reconfigure(encoding='utf-8')

app = Flask(__name__)
model = SentenceTransformer("BAAI/bge-m3")

server_name = sys.argv[1] if len(sys.argv) > 1 else "newEmbeddingServer"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5003

SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"

@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    print(f"process_embedding_request", flush=True)
    try:
        data = request.get_json()
        input_file = os.path.abspath(data.get("inputFile"))
        output_file = os.path.abspath(data.get("outputFile"))
        print(f"Input file (absolute path): {input_file}", flush=True)
        print(f"Output file (absolute path): {output_file}", flush=True)

        max_wait_time = 10
        wait_interval = 1
        file_has_been_found = False

        for attempt in range(max_wait_time):
            if os.path.isfile(input_file):
                print(f"Found the file: {input_file}", flush=True)
                file_has_been_found = True
                break
            else:
                print(f"Waiting for the file to appear... ({attempt + 1}/{max_wait_time})", flush=True)
                time.sleep(wait_interval)

        if not file_has_been_found:
            print(f"File not found after {max_wait_time} seconds: {input_file}", flush=True)
            return jsonify({"error": f"Input file does not exist after waiting: {input_file}"}), 400

        with open(input_file, "r", encoding="utf-8") as f:
            json_data = json.load(f)

        lines = json_data.get("chunks", [])

        if isinstance(lines, list):
            if all(isinstance(line, str) for line in lines):
                print("Detected list of strings", flush=True)
            elif all(isinstance(line, dict) and "content" in line for line in lines):
                print("Detected list of objects with 'content'", flush=True)
                lines = [line["content"] for line in lines]
            else:
                print("Invalid JSON structure: Expected list of strings or list of objects with 'content'", flush=True)
                return jsonify({"error": "'chunks' must be a list of strings or list of objects with 'content'"}), 400
        else:
            print("Invalid JSON structure: 'chunks' is not a list", flush=True)
            return jsonify({"error": "'chunks' must be a list"}), 400

        total = len(lines)
        if total == 0:
            return jsonify({"error": "Input file is empty"}), 400

        print(f"Rozpoczynam generowanie embeddingów ({total} linii)...", flush=True)

        results = []
        for i, line in enumerate(lines, start=1):
            embedding = model.encode(line).tolist()
            results.append({
                "index": i - 1,
                "content": line,
                "embeddedContent": embedding
            })

            if i % max(1, total // 10) == 0 or i == total:
                print(f"[{i}/{total}] ({round(i / total * 100)}%) przetworzono...", flush=True)

            if i % 100 == 0 or i == total:
                with open(output_file, "w", encoding="utf-8") as f_out:
                    json.dump({"embeddedChunks": results}, f_out, ensure_ascii=False, indent=2)

        print(f"Zapisano {i} elementów do pliku tymczasowo ({output_file})", flush=True)

        print(f"Embedding zakończony. Finalnie zapisano {len(results)} elementów do {output_file}", flush=True)
        return jsonify({"message": "Embedding completed", "output": output_file}), 200

    except Exception as e:
        import traceback
        print(traceback.format_exc(), flush=True)
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
    print("Notifying Spring Boot that embedding server is ready...", flush=True)
    print(f"SPRING_BOOT_NOTIFY={SPRING_BOOT_NOTIFY}", flush=True)
    try:
        response = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print(f"Spring Boot responded: {response.status_code} {response.text}", flush=True)
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def run_server():
    notify_spring_boot()
    print(f"Embedding server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
