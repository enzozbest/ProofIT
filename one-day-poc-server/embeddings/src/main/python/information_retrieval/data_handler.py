import faiss
import pickle
import os
import pathlib

# Get the absolute path of the current directory
BASE_DIR = str(pathlib.Path(__file__).parent.parent.absolute())

# Define file paths relative to the base directory
FAISS_FILE = os.path.join(BASE_DIR, "faiss.index")
MAPPINGS_FILE = os.path.join(BASE_DIR, "mappings.pkl")
LUCENE_INDEX_DIR = os.path.join(BASE_DIR, "jsonld_index")
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

    # Ensure the Lucene index directory exists
    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    return index, vector_store

def save_data(index, vector_store):
    """
    Save data into disk for persistence.
    """
    faiss.write_index(index, FAISS_FILE)
    with open(MAPPINGS_FILE, "wb") as f:
        pickle.dump(vector_store, f)
    print(f"Saved FAISS index to {FAISS_FILE}, mappings to {MAPPINGS_FILE}, and Lucene index is maintained at {LUCENE_INDEX_DIR}.")
