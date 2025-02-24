from flask import Flask, request, jsonify
import faiss
import numpy as np
from flask_cors import CORS
from sentence_transformers import SentenceTransformer

VECTOR_DIMENSION = 512

app = Flask(__name__)
CORS(app)

dimension = VECTOR_DIMENSION
index = faiss.IndexFlatL2(dimension)

vector_store = {}

model = SentenceTransformer('all-MiniLM-L6-v2')

@app.route('/embeddings/embed', methods=['POST'])
def create_embedding():
    """
    Converts input text into vector embeddings using a huggingface sentence transformer.
    """

    data = request.json
    if "text" in data:
        prompt = data["text"]
        try:
            embedding = model.encode(prompt)
            embedding_list = embedding.tolist()

            return jsonify({
                "status": "success",
                "embedding": embedding_list
            })
        except Exception as e:
            return jsonify({
                "status": "error",
                "message": f"Error generating embedding: {str(e)}"
            })

    else:
        return jsonify({
            "status": "error",
            "message": "No prompt provided"
        })

@app.route('/embeddings/new', methods=['POST'])
def new_embedding():
    data = request.json
    embedded = create_embedding(request)
    success = store_embedding(name = data["name"], vector = embedded)
    if not success:
        return jsonify({"status": "error", "message": "Index is not trained."})
    return jsonify({"status": "success", "message": "Vector added to the DB."})

def store_embedding(name: str, vector: np.ndarray) -> Bool:
    if not index.is_trained:
        return False

    vector = vector.reshape(1, -1)
    index.add(vector)
    vector_store[len(vector_store)] = name
    return True

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
