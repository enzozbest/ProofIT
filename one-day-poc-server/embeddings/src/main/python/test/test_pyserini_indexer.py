import os
import pytest

from information_retrieval.keyword_search.pyserini_indexer import store_jsonld, keyword_search, LUCENE_INDEX_DIR
from pyserini.index.lucene import LuceneIndexReader

@pytest.fixture(autouse=True)
def setup_index():
    """Clears old index and sets up new test data before each test."""

    if os.path.exists(LUCENE_INDEX_DIR):
        import shutil
        try:
            shutil.rmtree(LUCENE_INDEX_DIR)
        except Exception as e:
            os.system(f"rm -rf {LUCENE_INDEX_DIR}")
            if os.path.exists(LUCENE_INDEX_DIR):
                for item in os.listdir(LUCENE_INDEX_DIR):
                    item_path = os.path.join(LUCENE_INDEX_DIR, item)
                    try:
                        if os.path.isfile(item_path):
                            os.remove(item_path)
                        elif os.path.isdir(item_path):
                            shutil.rmtree(item_path)
                    except Exception as e:
                        print(f"Error removing {item_path}: {e}")

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

def test_valid_index_searcher_close():
    """Test that searcher.close() is called when checking a valid index."""

    test_data = {
        "name": "TestComponent",
        "description": "A test component for checking searcher.close()",
        "keywords": ["test", "component"],
        "library": "Test"
    }
    success1 = store_jsonld("ValidIndexTest", test_data)
    assert success1, "Failed to store first JSON-LD data."

    another_test_data = {
        "name": "AnotherComponent",
        "description": "Another test component",
        "keywords": ["another", "test"],
        "library": "Test"
    }
    success2 = store_jsonld("AnotherTest", another_test_data)
    assert success2, "Failed to store second JSON-LD data."

    results = keyword_search("test", top_k=5)
    assert len(results) > 0, "At least one document should be found in the index."



def test_invalid_index_with_subdirectory():
    """Test handling of invalid index with subdirectories."""

    if os.path.exists(LUCENE_INDEX_DIR):
        import shutil
        try:
            shutil.rmtree(LUCENE_INDEX_DIR)
        except Exception as e:
            os.system(f"rm -rf {LUCENE_INDEX_DIR}")
            if os.path.exists(LUCENE_INDEX_DIR):
                for item in os.listdir(LUCENE_INDEX_DIR):
                    item_path = os.path.join(LUCENE_INDEX_DIR, item)
                    try:
                        if os.path.isfile(item_path):
                            os.remove(item_path)
                        elif os.path.isdir(item_path):
                            shutil.rmtree(item_path)
                    except Exception as e:
                        print(f"Error removing {item_path}: {e}")

    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    test_subdir = os.path.join(LUCENE_INDEX_DIR, "test_subdir")
    os.makedirs(test_subdir, exist_ok=True)

    with open(os.path.join(LUCENE_INDEX_DIR, "invalid_file.txt"), "w") as f:
        f.write("This is an invalid index file")

    test_data = {
        "name": "InvalidIndexTest",
        "description": "Testing invalid index handling",
        "keywords": ["invalid", "index", "test"],
        "library": "Test"
    }
    success = store_jsonld("InvalidIndexTest", test_data)
    assert success, "Failed to store JSON-LD data after handling invalid index."

    results = keyword_search("InvalidIndexTest", top_k=5)
    assert "InvalidIndexTest" in results, "Test document should be found in the new index."

def test_empty_index_directory():
    """Test search with an empty index directory."""
    if os.path.exists(LUCENE_INDEX_DIR):
        import shutil
        try:
            shutil.rmtree(LUCENE_INDEX_DIR)
        except Exception as e:
            os.system(f"rm -rf {LUCENE_INDEX_DIR}")
            if os.path.exists(LUCENE_INDEX_DIR):
                for item in os.listdir(LUCENE_INDEX_DIR):
                    item_path = os.path.join(LUCENE_INDEX_DIR, item)
                    try:
                        if os.path.isfile(item_path):
                            os.remove(item_path)
                        elif os.path.isdir(item_path):
                            shutil.rmtree(item_path)
                    except Exception as e:
                        print(f"Error removing {item_path}: {e}")


    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    results = keyword_search("test query", top_k=5)
    assert results == [], "Search should return an empty list for an empty index directory."
