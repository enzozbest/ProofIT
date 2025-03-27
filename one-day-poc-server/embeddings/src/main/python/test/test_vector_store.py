import pytest
import numpy as np
import faiss

from information_retrieval.vector_search import vector_store
from information_retrieval.vector_search.vector_store import semantic_search, store_embedding

@pytest.fixture
def setup_faiss_index():
    """Fixture to set up a FAISS index for testing."""

    vector_store.index = faiss.IndexFlatIP(384) 
    vector_store.store = {}


@pytest.fixture
def sample_embedding():
    """Fixture to create a sample embedding."""
    return np.random.rand(384).astype(np.float32)


def test_semantic_search_empty_index(setup_faiss_index):
    """Test searching when the index is empty."""
    data = {"embedding": np.random.rand(384).tolist(), "topK": 5}
    result = semantic_search(data["embedding"], data["topK"])
    assert result == [], "Semantic search should return [] when index is empty"


def test_semantic_search_valid(setup_faiss_index, sample_embedding):
    """Test searching when the index contains valid embeddings."""
    store_embedding("test_name", sample_embedding)

    data = {"embedding": sample_embedding.tolist(), "topK": 1}
    result = semantic_search(data["embedding"], data["topK"])

    assert result != -1, "Semantic search should return results when embeddings exist"
    assert "test_name" in result, "Stored name should be retrievable in search results"

def test_store_embedding_success(setup_faiss_index, sample_embedding):
    """Test storing a valid embedding."""
    assert store_embedding("test_name", sample_embedding), "store_embedding should return True"
    assert len(vector_store.store) == 1, "Vector store should contain one entry"
    assert "test_name" in vector_store.store.values(), "Name should be stored in vector store"


def test_store_embedding_untrained_index():
    """Test storing an embedding when the index is untrained (should fail)."""
    vector_store.index = faiss.IndexIVFFlat(faiss.IndexFlatL2(384), 384, 10) 
    sample_vector = np.random.rand(384).astype(np.float32)
    result = store_embedding("test_name", sample_vector)
    assert not result, "store_embedding should return False if index is untrained"
