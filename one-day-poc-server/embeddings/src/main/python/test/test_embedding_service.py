import pytest
import numpy as np
from unittest import mock

import information_retrieval
from information_retrieval.embedding_service import app
from information_retrieval.vector_search import vector_store
from information_retrieval.keyword_search import pyserini_indexer

@pytest.fixture
def client():
    """Fixture to provide a test client for the Flask app."""
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


@pytest.fixture
def mock_vector_store():
    """Mock the vector_store module."""
    with mock.patch.object(vector_store, "store_embedding", return_value=True) as mock_store_embedding, \
            mock.patch.object(vector_store, "semantic_search", return_value=["test_result"]) as mock_semantic_search:
        yield {
            "store_embedding": mock_store_embedding,
            "semantic_search": mock_semantic_search
        }

@pytest.fixture
def mock_pyserini_indexer():
    """Mock the pyserini_indexer module."""
    with mock.patch.object(pyserini_indexer, "store_jsonld") as mock_store_jsonld, \
            mock.patch.object(pyserini_indexer, "keyword_search") as mock_keyword_search:
        yield {
            "keyword_search": mock_keyword_search,
            "store_jsonld": mock_store_jsonld,
        }

@pytest.fixture
def mock_embed():
    """Mock the embed function."""
    with mock.patch("information_retrieval.vector_search.embedder.embed", return_value=np.random.rand(384).tolist()) as mock_func:
        yield mock_func

@pytest.fixture
def mock_embed_error():
    """Mock the embed function."""
    with mock.patch("information_retrieval.vector_search.embedder.embed", return_value=None) as mock_func:
        yield mock_func


@pytest.fixture
def mock_load_data():
    """Mock the load_data function for app startup."""
    with mock.patch("information_retrieval.data_handler.load_data", return_value=(mock.Mock(), {})) as mock_func:
        yield mock_func


@pytest.fixture
def mock_save_data():
    """Mock the save_data function called on app exit."""
    with mock.patch("information_retrieval.data_handler.save_data") as mock_func:
        yield mock_func


# --- TESTS ---

def test_embed_route_success(client, mock_embed):
    """Test embedding route with valid text input."""
    response = client.post("/embed", json={"text": "Hello world"})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "success"
    assert "embedding" in data


def test_embed_route_no_text(client):
    """Test embedding route when no text is provided."""
    response = client.post("/embed", json={})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "error"
    assert data["message"] == "No prompt provided"

# def test_embed_route_failure(client, mock_embed_error):
#     """Test embedding route when embedding fails."""
#     response = client.post("/embed", json={"text": "Hello world"})
#     data = response.get_json()
#
#     assert response.status_code == 200
#     assert data["status"] == "error"
#     assert "Error embedding" in data["message"]


def test_embed_and_store_route_success(client, mock_vector_store, mock_embed, mock_pyserini_indexer):
    """Test embedding and storing a vector successfully."""
    response = client.post("/new", json={"text": {"id":"my_id", "description":"Test"}, "name": "Test"})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "success"
    assert data["message"] == "New template stored successfully!"
    mock_vector_store["store_embedding"].assert_called_once()
    mock_pyserini_indexer["store_jsonld"].assert_called_once()


def test_embed_and_store_route_no_text(client):
    """Test embedding and storing when no text is provided."""
    response = client.post("/new", json={"name": "Test"})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "error"
    assert data["message"] == "No prompt provided"


# def test_embed_and_store_route_failure(client, mock_vector_store, mock_embed):
#     """Test embedding and storing when the index is not trained."""
#     mock_vector_store["store_embedding"].return_value = False  # Simulate failure
#     response = client.post("/new", json={"text": {"id":"my_id", "description":"Test"}, "name": "Test"})
#     data = response.get_json()
#
#     assert response.status_code == 200
#     assert data["status"] == "error"
#     assert data["message"] == "Index is not trained."


# def test_search_route_success(client, mock_vector_store):
#     """Test search with valid embedding input and query."""
#     response = client.post("/search", json={"embedding": np.random.rand(384).tolist(), "query": "Test"})
#     data = response.get_json()
#
#     assert response.status_code == 200
#     assert data["status"] == "success"
#     assert "matches" in data
#     assert data["matches"] == ["test_result"]


def test_search_route_no_embedding(client):
    """Test search when no embedding is provided."""
    response = client.post("/search", json={"query": "Test"})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "error"
    assert data["message"] == "No embedding provided for semantic search!"

def test_search_route_no_query(client):
    """Test search when no embedding is provided."""
    response = client.post("/search", json={"embedding": np.random.rand(384).tolist()})
    data = response.get_json()

    assert response.status_code == 200
    assert data["status"] == "error"
    assert data["message"] == "No query provided for keyword search!"


# def test_search_route_no_matches(client, mock_vector_store):
#     """Test search when no matches are found."""
#     mock_vector_store["semantic_search"].return_value = -1  # Simulate no matches found
#     response = client.post("/search", json={"embedding": np.random.rand(384).tolist(), "query": "Test"})
#     data = response.get_json()
#
#     assert response.status_code == 200
#     assert data["status"] == "success"


# def test_app_starts_up_once(client, mock_load_data):
#     """Test that the application loads data only once on the first request."""
#     information_retrieval.embedding_service.first_request = True
#     response = client.get("/")
#     assert response.status_code == 404
#
#     mock_load_data.assert_called_once()


@mock.patch("information_retrieval.data_handler.save_data")
def test_app_exit_saves_data(mock_save_data):
    """Test that save_data is called on app exit."""
    import atexit
    import information_retrieval.data_handler
    atexit.unregister(information_retrieval.data_handler.save_data)
    atexit.register(information_retrieval.data_handler.save_data, information_retrieval.vector_search.vector_store.index, information_retrieval.vector_search.vector_store.store)
    atexit._run_exitfuncs()  # Simulate app exit
    mock_save_data.assert_called_once()
