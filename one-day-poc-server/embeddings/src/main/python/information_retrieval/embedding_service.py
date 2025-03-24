import json

import numpy as np
from flask import Flask, jsonify, request
from flask_cors import CORS

from information_retrieval.data_handler import load_data, save_data
from information_retrieval.keyword_search import pyserini_indexer as pi
from information_retrieval.vector_search import embedder as emb, vector_store as vs

app = Flask(__name__)
CORS(app)

first_request = True
@app.before_request
def startup_once():
    global first_request
    if first_request:
        vs.index, vs.store = load_data()
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
    embedding = emb.embed(text)
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
    embedding = emb.embed(jsonld)
    vector_success = vs.store_embedding(name, vector = np.array(embedding))
    keyword_success = pi.store_jsonld(name, json.loads(jsonld))
    if not vector_success or not keyword_success:
        return jsonify({"status": "error", "message": f"Failed to store template: Vector DB: {vector_success}, Keyword DB: {keyword_success}"})

    # Save data to disk after successful storage
    save_data(vs.index, vs.store)

    return jsonify({"status": "success", "message": "New template stored successfully!"})

@app.route('/search', methods=['POST'])
def search_route():
    data = request.json
    if not "embedding" in data:
        return jsonify({"status": "error", "message": "No embedding provided for semantic search!"})
    if not "query" in data:
        return jsonify({"status": "error", "message": "No query provided for keyword search!"})

    top_k = data.get("top_k", 5)
    vector_results = vs.semantic_search(data["embedding"], top_k=top_k)
    keyword_results = pi.keyword_search(data["query"], top_k=top_k)
    results = list(set(vector_results + keyword_results))
    return jsonify({"status": "success", "matches": results})
