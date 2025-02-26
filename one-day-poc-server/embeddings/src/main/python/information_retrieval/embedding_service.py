import atexit

import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from information_retrieval.data_handler import load_data, save_data
from information_retrieval.vector_search.embedder import embed
from information_retrieval.vector_search import vector_store
from information_retrieval.keyword_search import pyserini_indexer
app = Flask(__name__)
CORS(app)

first_request = True

@app.before_request
def startup_once():
    global first_request
    if first_request:
        vector_store.index, vector_store.store = load_data()
        first_request = False

@app.route('/embed', methods=['POST'])
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

@app.route('/new', methods=['POST'])
def new_template_route():
    data = request.json
    if not "text" in data:
        return jsonify({"status": "error", "message": "No prompt provided"})

    jsonld = data["text"]
    name = data["name"]
    embedding = embed(jsonld)
    vector_success = vector_store.store_embedding(name, vector = np.array(embedding))
    keyword_success = pyserini_indexer.store_jsonld(name, jsonld)
    if not vector_success and keyword_success:
        return jsonify({"status": "error", "message": "Index is not trained."})
    return jsonify({"status": "success", "message": "New template stored successfully!"})

@app.route('/search', methods=['POST'])
def search_route():
    data = request.json
    if not "embedding" in data:
        return jsonify({"status": "error", "message": "No embedding provided for semantic search!"})
    if not "query" in data:
        return jsonify({"status": "error", "message": "No query provided for keyword search!"})

    top_k = data.get("top_k", 5)
    vector_results = vector_store.semantic_search(data["embedding"], top_k=top_k)
    keyword_results = pyserini_indexer.keyword_search(data["query"], top_k=top_k)
    results = list(set(vector_results + keyword_results))
    if len(results) <= 0:
        return jsonify({"status": "error", "message": f"No matches found for : {str(data)}"})
    return jsonify({"status": "success", "matches": results})


if __name__ == '__main__':
    atexit.register(save_data, vector_store.index, vector_store.store)
    app.run(host="0.0.0.0", port=7000)
