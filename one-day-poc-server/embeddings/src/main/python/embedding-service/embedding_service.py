from flask import Flask, request, jsonify
import faiss
import numpy as np
from flask_cors import CORS
from sentence_transformers import SentenceTransformer
import pickle

FAISS_FILE = "faiss.index"
MAPPINGS_FILE = "mappings.pkl"

VECTOR_DIMENSION = 512

app = Flask(__name__)
CORS(app)

index = None
vector_store = {}

@app.before_first_request
def load_data():
    """
    Retrieve persisted data from disk. These will be embeddings and corresponding mappings.
    """
    global index, vector_store
    try:
        index = faiss.read_index(FAISS_FILE)
        with open(MAPPINGS_FILE, "rb") as f:
            vector_store = pickle.load(f)
    except FileNotFoundError:
            print("Could not load data. Creating new index and mapping.")
            index = faiss.IndexFlatL2(VECTOR_DIMENSION)
            vector_store = {}

def save_data():
    """
    Save data into disk for persistence.
    """
    faiss.write_index(index, FAISS_FILE)
    with open(MAPPINGS_FILE, "wb") as f:
        pickle.dump(vector_store, f)
    print("Saved index and mapping data to disk.")

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

@app.route('/embeddings/semantic-search', methods=['POST'])
def semantic_search():
    data = request.json
    base = np.array(data["embedding"], dtype=np.float32).reshape(1, -1)
    top_k = data.get("topK", 5)

    if index.ntotal == 0:
        return jsonify({"status": "error", "message": "Index is empty."})

    distances, indices = index.search(base, top_k)
    results = str([vector_store[idx] for idx in indices[0] if idx in vector_store])
    return jsonify({"status": "success", "matches": results})

def store_embedding(name: str, vector: np.ndarray) -> bool:
    if not index.is_trained:
        return False
    vector = vector.reshape(1, -1)
    index.add(vector)
    vector_store[len(vector_store)] = name
    return True

if __name__ == '__main__':
    atexit.register(save_data)
    app.run(host="0.0.0.0", port=7000)
