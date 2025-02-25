from sentence_transformers import SentenceTransformer
import numpy as np

model = SentenceTransformer('all-MiniLM-L6-v2')

def embed(text: str):
    """Converts input text into vector embeddings using a huggingface sentence transformer."""
    try:
        return normalize(model.encode(text).tolist())
    except Exception as e:
        return None

def normalize(embedding: list[float]) -> list[float]:
    """Normalize the embedding vectors so that they sum up to 1 (or very close to)."""
    embedding = np.array(embedding)
    norm = np.linalg.norm(embedding)
    return (embedding / norm).tolist() if norm != 0 else embedding