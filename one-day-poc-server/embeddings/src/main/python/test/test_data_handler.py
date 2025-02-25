import pytest
import faiss
from unittest import mock
from embedding_service.data_handler import load_data, save_data, FAISS_FILE, MAPPINGS_FILE, VECTOR_DIMENSION  # Replace 'your_module'

@pytest.fixture
def mock_faiss_index():
    """Create a mock FAISS index for testing."""
    return faiss.IndexFlatIP(VECTOR_DIMENSION)

@pytest.fixture
def mock_vector_store():
    """Create a mock dictionary for storing vector mappings."""
    return {"test_id": [1.0] * VECTOR_DIMENSION}

@mock.patch("faiss.read_index")
@mock.patch("builtins.open", new_callable=mock.mock_open)
@mock.patch("pickle.load")
def test_load_data_success(mock_pickle_load, mock_open, mock_faiss_read, mock_faiss_index, mock_vector_store):
    """Test loading FAISS index and mappings successfully."""
    mock_faiss_read.return_value = mock_faiss_index
    mock_pickle_load.return_value = mock_vector_store

    index, vector_store = load_data()

    assert isinstance(index, faiss.IndexFlatIP), "Index should be a FAISS IndexFlatIP object"
    assert vector_store == mock_vector_store, "Vector store should match the loaded data"

@mock.patch("faiss.read_index", side_effect=Exception("FAISS Load Error"))
@mock.patch("builtins.open", new_callable=mock.mock_open)
@mock.patch("pickle.load", side_effect=Exception("Pickle Load Error"))
def test_load_data_failure(mock_pickle_load, mock_open, mock_faiss_read):
    """Test handling of missing or invalid FAISS index and mappings."""
    index, vector_store = load_data()

    assert isinstance(index, faiss.IndexFlatIP), "Failed index load should create a new FAISS index"
    assert vector_store == {}, "Failed vector store load should return an empty dictionary"

@mock.patch("faiss.read_index", return_value=None)
@mock.patch("builtins.open", new_callable=mock.mock_open)
@mock.patch("pickle.load", return_value=None)
def test_load_data_returns_none(mock_pickle_load, mock_open, mock_faiss_read):
    """Test handling when the files exist but contain None."""
    index, vector_store = load_data()

    assert isinstance(index, faiss.IndexFlatIP), "If FAISS index is None, a new index should be created"
    assert vector_store == {}, "If the vector store is None, it should return an empty dictionary"

@mock.patch("faiss.read_index", return_value=None)
@mock.patch("builtins.open", new_callable=mock.mock_open, read_data=b"")
@mock.patch("pickle.load", side_effect=EOFError)  # Simulates empty file
def test_load_data_empty_file(mock_pickle_load, mock_open, mock_faiss_read):
    """Test handling of empty files."""
    index, vector_store = load_data()

    assert isinstance(index, faiss.IndexFlatIP), "Empty FAISS file should result in a new index"
    assert vector_store == {}, "Empty pickle file should result in an empty dictionary"

@mock.patch("faiss.write_index")
@mock.patch("builtins.open", new_callable=mock.mock_open)
@mock.patch("pickle.dump")
def test_save_data(mock_pickle_dump, mock_open, mock_faiss_write, mock_faiss_index, mock_vector_store):
    """Test saving FAISS index and mappings."""
    save_data(mock_faiss_index, mock_vector_store)

    mock_faiss_write.assert_called_once_with(mock_faiss_index, FAISS_FILE), "FAISS index should be written to disk"
    mock_open.assert_called_once_with(MAPPINGS_FILE, "wb"), "Mapping file should be opened for writing"
    mock_pickle_dump.assert_called_once_with(mock_vector_store, mock_open()), "Vector store should be serialized"

