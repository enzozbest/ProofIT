import numpy as np
from data_handler import load_data

index, vector_store = None, {}

def semantic_search(data):
    base = np.array(data["embedding"], dtype=np.float32).reshape(1, -1)
    top_k = data.get("topK", 5)

    if index.ntotal == 0:
        return -1

    distances, indices = index.search(base, top_k)
    results = str([vector_store[idx] for idx in indices[0] if idx in vector_store])
    return results

def store_embedding(name: str, vector: np.array) -> bool:
    if not index.is_trained:
        return False
    vector = vector.reshape(1, -1)
    index.add(vector)
    vector_store[len(vector_store)] = name
    return True