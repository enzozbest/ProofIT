import numpy as np

index, store = None, {}

def semantic_search(embedding: list, top_k: int):
    base = np.array(embedding, dtype=np.float32).reshape(1, -1)
    if index.ntotal == 0:
        return []

    distances, indices = index.search(base, top_k)
    results = [store[idx] for idx in indices[0] if idx in store]
    return results

def store_embedding(name: str, vector: np.array) -> bool:
    if not index.is_trained:
        return False
    vector = vector.reshape(1, -1)
    index.add(vector)
    store[len(store)] = name
    return True