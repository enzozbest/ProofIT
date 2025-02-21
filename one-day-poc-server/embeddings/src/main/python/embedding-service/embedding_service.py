from flask import Flask, request, jsonify
import faiss
import numpy as np
from flask_cors import CORS

VECTOR_DIMENSION = 512

app = Flask(__name__)
CORS(app)

dimension = VECTOR_DIMENSION
index = faiss.IndexFlatL2(dimension)

vector_store = {}

@app.route('/embeddings/new', methods=['POST'])
def new_embedding():
    data = request.json
    name = data["name"]
    vector = np.array(data["vector"], dtype=np.float32).reshape(1, -1)

    if index.is_trained():
        index.add(vector)
        vector_store[len(vector_store)] = name
        return jsonify({"status": "success", "message": "Vector added."})
    else:
        return jsonify({"status": "error", "message": "Index is not trained."})

@app.route('/embeddings/', methods=['GET'])
def query_vector():
    data = request.json
    query_vector = np.array(data["vector"], dtype=np.float32).reshape(1, -1)
    top_k = data.get("topK", 5)

    if index.ntotal == 0:
        return jsonify({"status": "error", "message": "Index is empty."})

    distances, indices = index.search(query_vector, top_k)
    results = [vector_store[idx] for idx in indices[0] if idx in vector_store]

    return jsonify({"status": "success", "matches": results})


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=7000)
