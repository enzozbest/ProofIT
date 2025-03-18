import os
import pytest

from information_retrieval.keyword_search.pyserini_indexer import store_jsonld, keyword_search, LUCENE_INDEX_DIR
from pyserini.index.lucene import LuceneIndexReader

@pytest.fixture(autouse=True)
def setup_index():
    """Clears old index and sets up new test data before each test."""
    # Ensure the directory is completely removed before creating a new one
    if os.path.exists(LUCENE_INDEX_DIR):
        import shutil
        try:
            shutil.rmtree(LUCENE_INDEX_DIR)
        except Exception as e:
            print(f"Error removing directory: {e}")
            # If shutil.rmtree fails, try using os.system as a fallback
            os.system(f"rm -rf {LUCENE_INDEX_DIR}")

    # Ensure the directory doesn't exist before creating it
    assert not os.path.exists(LUCENE_INDEX_DIR), "Failed to remove old index directory"

    # Create a fresh directory
    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    test_data = {
        "name": "LoginForm",
        "description": "A responsive login form with email, password, and Google OAuth.",
        "keywords": ["login", "authentication", "oauth"],
        "library": "React"
    }
    success = store_jsonld("TestID", test_data)
    assert success, "Failed to store JSON-LD data."

def test_index_creation():
    assert os.path.exists(LUCENE_INDEX_DIR), "Lucene index directory was not created."

    # Check if the index is readable.
    reader = LuceneIndexReader(LUCENE_INDEX_DIR)
    num_docs = reader.stats()["documents"]
    assert num_docs > 0, "Lucene index should have at least one document."

def test_store_json_ld_with_non_dict():
    os.system(f"rm -rf {LUCENE_INDEX_DIR}")
    success = store_jsonld("TestID", "Test")
    assert not success, "Function failed to recognise data was not a dict"

def test_keyword_search():
    query = "Google OAuth login"
    results = keyword_search(query, top_k=1)
    assert isinstance(results, list), "Search should return a list of results."
    assert "TestID" in results, "Expected 'TestID' to appear in search results."

def test_empty_search():
    query = "Blockchain smart contract"
    results = keyword_search(query, top_k=5)
    assert isinstance(results, list), "Search should return a list."
    assert len(results) == 0, "Search should return an empty list for unrelated queries."

def test_search_when_indices_are_not_available():
    query = "Blockchain smart contract"
    os.system(f"rm -rf {LUCENE_INDEX_DIR}")
    results = keyword_search(query, top_k=5)
    assert results == [], "Search should return an empty list if LuceneIndex is not available of results."
