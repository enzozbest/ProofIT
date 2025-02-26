import faiss
import pickle

FAISS_FILE = "faiss.index"
MAPPINGS_FILE = "mappings.pkl"
VECTOR_DIMENSION = 384

def load_data():
    """
    Retrieve persisted data from disk. These will be embeddings and corresponding mappings.
    If the data is missing or invalid (e.g., files exist but are empty or contain null),
    new objects will be created.
    """
    try:
        index = faiss.read_index(FAISS_FILE)
        if index is None:
            index = faiss.IndexFlatIP(VECTOR_DIMENSION)
    except Exception as e:
        print(f"Could not load FAISS index from {FAISS_FILE} ({e}). Creating new index.")
        index = faiss.IndexFlatIP(VECTOR_DIMENSION)

    try:
        with open(MAPPINGS_FILE, "rb") as f:
            vector_store = pickle.load(f)
        if vector_store is None:
            vector_store = {}
    except Exception as e:
        print(f"Could not load mapping from {MAPPINGS_FILE} ({e}). Creating new mapping.")
        vector_store = {}

    return index, vector_store

def save_data(index, vector_store):
    """
    Save data into disk for persistence.
    """
    faiss.write_index(index, FAISS_FILE)
    with open(MAPPINGS_FILE, "wb") as f:
        pickle.dump(vector_store, f)
    print("Saved index and mapping data to disk.")