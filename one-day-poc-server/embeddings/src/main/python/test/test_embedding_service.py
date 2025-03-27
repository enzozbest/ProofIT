import pytest

import information_retrieval.embedding_service
from information_retrieval.embedding_service import app
import information_retrieval.embedding_service as es
import information_retrieval.data_handler as dh

@pytest.fixture
def client(monkeypatch):
    """
    Creates a Flask test client and patches certain functions so that
    the 'before_request' hook and other external dependencies don't
    interfere with tests.
    """
    monkeypatch.setattr(es, "first_request", True)
    monkeypatch.setattr(dh, "load_data", lambda: (None, None))
    app.config["TESTING"] = True
    return app.test_client()


def test_embed_no_text(client):
    """
    Test the /embed route with no "text" key in the payload.
    """
    response = client.post("/embed", json={})
    data = response.get_json()
    assert data["status"] == "error"
    assert data["message"] == "No prompt provided"

def test_embed_success(client, monkeypatch):
    """
    Test the /embed route with a successful embedding response.
    """
    monkeypatch.setattr(information_retrieval.embedding_service.emb, "embed", lambda text: [0.1, 0.2, 0.3])

    payload = {"text": "test prompt"}
    response = client.post("/embed", json=payload)
    data = response.get_json()
    assert data["status"] == "success"
    assert "embedding" in data
    assert data["embedding"] == [0.1, 0.2, 0.3]

def test_embed_fail(client, monkeypatch):
    """
    Test the /embed route with an error response.
    """
    monkeypatch.setattr(information_retrieval.embedding_service.emb, "embed", lambda text: [])

    payload = {"text": "test prompt"}
    response = client.post("/embed", json=payload)
    data = response.get_json()
    assert data["status"] == "error"
    assert "message" in data
    assert data["message"] == "Error embedding: test prompt"


def test_new_no_text(client):
    """
    Test the /new route when "text" is missing from the payload.
    """
    payload = {"name": "SomeName"}
    response = client.post("/new", json=payload)
    data = response.get_json()
    assert data["status"] == "error"
    assert data["message"] == "No prompt provided"


def test_new_success(client, monkeypatch):
    """
    Test the /new route when everything works as expected:
    - embed() returns a valid vector
    - vector_store.store_embedding() returns True
    - pyserini_indexer.store_jsonld() returns True
    """
    def fake_embed(text):
        return [0.1, 0.2, 0.3]

    monkeypatch.setattr(information_retrieval.embedding_service.emb, "embed", fake_embed)
    monkeypatch.setattr(information_retrieval.embedding_service.vs,
                        "store_embedding", lambda name, vector: True)
    monkeypatch.setattr(information_retrieval.embedding_service.pi,
                        "store_jsonld", lambda name, jsonld: True)


    payload = {
        "text": {"name": "LoginForm", "description": "desc"},
        "name": "LoginForm"
    }
    response = client.post("/new", json=payload)
    data = response.get_json()
    assert data["status"] == "success"
    assert data["message"] == "New template stored successfully!"


def test_new_vector_failure(client, monkeypatch):
    """
    Test the /new route when vector_store.store_embedding() fails
    but pyserini_indexer.store_jsonld() succeeds.
    """
    def fake_embed(text):
        return [0.1, 0.2, 0.3]

    monkeypatch.setattr(information_retrieval.embedding_service.emb, "embed", fake_embed)
    monkeypatch.setattr(information_retrieval.embedding_service.vs,
                        "store_embedding", lambda name, vector: False)
    monkeypatch.setattr(information_retrieval.embedding_service.pi,
                        "store_jsonld", lambda name, jsonld: True)

    payload = {
        "text": {"name": "LoginForm", "description": "desc"},
        "name": "LoginForm"
    }
    response = client.post("/new", json=payload)
    data = response.get_json()
    assert data["status"] == "error"
    assert data["message"] == "Failed to store template: Vector DB: False, Keyword DB: True"


def test_search_no_embedding(client):
    """
    Test the /search route when "embedding" is missing.
    """
    response = client.post("/search", json={"query": "test"})
    data = response.get_json()
    assert data["status"] == "error"
    assert data["message"] == "No embedding provided for semantic search!"


def test_search_no_query(client):
    """
    Test the /search route when "query" is missing.
    """
    response = client.post("/search", json={"embedding": [0.1, 0.2, 0.3]})
    data = response.get_json()
    assert data["status"] == "error"
    assert data["message"] == "No query provided for keyword search!"


def test_search_success(client, monkeypatch):
    """
    Test the /search route when both semantic_search and keyword_search return results.
    """
    monkeypatch.setattr(information_retrieval.embedding_service.vs,
                        "semantic_search", lambda embb, top_k=5: ["doc1"])
    monkeypatch.setattr(information_retrieval.embedding_service.pi,
                        "keyword_search", lambda q, top_k=5: ["doc2"])

    payload = {"embedding": [0.1, 0.2, 0.3], "query": "test", "top_k": 5}
    response = client.post("/search", json=payload)
    data = response.get_json()
    assert data["status"] == "success"
    assert set(data["matches"]) == {"doc1", "doc2"}


