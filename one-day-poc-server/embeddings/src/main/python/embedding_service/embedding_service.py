import atexit

import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from embedding_service.data_handler import load_data, save_data
from embedding_service.embedder import embed
from embedding_service import vector_store

app = Flask(__name__)
CORS(app)

first_request = True

@app.before_request
def startup_once():
    global first_request
    if first_request:
        vector_store.index, vector_store.store = load_data()
        first_request = False

@app.route('/embeddings/embed', methods=['POST'])
def embed_route():
    data = request.json
    if not "text" in data:
        return jsonify({
            "status": "error",
            "message": "No prompt provided"
        })
    text = data["text"]
    embedding = embed(text)
    if not embedding:
        return jsonify({"status": "error", "message": f"Error embedding: {text}"})
    return jsonify({"status": "success", "embedding": embedding})

@app.route('/embeddings/new', methods=['POST'])
def embed_and_store_route():
    data = request.json
    if not "text" in data:
        return jsonify({"status": "error", "message": "No prompt provided"})

    embedding = embed(data["text"])
    success = vector_store.store_embedding(name = data["name"], vector = np.array(embedding))
    if not success:
        return jsonify({"status": "error", "message": "Index is not trained."})
    return jsonify({"status": "success", "message": "Vector added to the DB successfully."})

@app.route('/embeddings/semantic-search', methods=['POST'])
def semantic_search_route():
    data = request.json
    if not "embedding" in data:
        return jsonify({"status": "error", "message": "No embedding provided for semantic search!"})

    results = vector_store.semantic_search(data)
    if results == -1:
        return jsonify({"status": "error", "message": f"No matches found for : {str(data)}"})
    return jsonify({"status": "success", "matches": results})


if __name__ == '__main__':
    atexit.register(save_data, vector_store.index, vector_store.store)
    app.run(host="0.0.0.0", port=7000)
