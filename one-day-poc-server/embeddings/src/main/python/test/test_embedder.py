import pytest
import numpy as np
from information_retrieval.vector_search.embedder import embed, normalize


def test_embed_valid_text():
    """Test that embedding a valid text returns a non-empty list."""
    text = "Hello, this is a test."
    embedding = embed(text)

    assert embedding is not None, "Embedding function should return a non-null value"
    assert isinstance(embedding, list), "Embedding should be returned as a list"
    assert all(isinstance(value, float) for value in embedding), "All elements should be floats"


def test_embed_empty_string():
    """Test that embedding an empty string returns a valid normalized vector."""
    text = ""
    embedding = embed(text)

    assert embedding is not None, "Embedding should return a value even for an empty string"
    assert isinstance(embedding, list), "Embedding should be a list"
    assert all(isinstance(value, float) for value in embedding), "All elements should be floats"


def test_embed_invalid_input():
    """Test that embedding a non-string input returns None."""
    assert embed(None) is None, "Embedding None should return None"
    assert embed(123) is None, "Embedding an integer should return None"
    assert embed(["list", "of", "words"]) is None, "Embedding a list should return None"


def test_normalize_valid_vector():
    """Test normalization of a valid vector."""
    vector = [3.0, 4.0]
    normalized = normalize(vector)

    assert isinstance(normalized, list), "Normalized output should be a list"
    assert len(normalized) == len(vector), "Normalized vector should have the same length"
    assert np.isclose(np.linalg.norm(normalized), 1.0), "Normalized vector should have a unit norm"


def test_normalize_zero_vector():
    """Test normalization of a zero vector, which should remain unchanged."""
    vector = [0.0, 0.0, 0.0]
    normalized = normalize(vector)

    assert isinstance(normalized, list), "Output should be a list"
    assert np.allclose(normalized, vector), "Zero vector should remain unchanged"


def test_normalize_negative_values():
    """Test normalization of a vector containing negative values."""
    vector = [-1.0, -2.0, -3.0]
    normalized = normalize(vector)

    assert isinstance(normalized, list), "Output should be a list"
    assert np.isclose(np.linalg.norm(normalized), 1.0), "Normalized vector should have a unit norm"


def test_normalize_large_values():
    """Test normalization of a vector with large values to ensure numerical stability."""
    vector = [1e6, 2e6, 3e6]
    normalized = normalize(vector)

    assert isinstance(normalized, list), "Output should be a list"
    assert np.isclose(np.linalg.norm(normalized), 1.0), "Normalized vector should have a unit norm"


if __name__ == "__main__":
    pytest.main()