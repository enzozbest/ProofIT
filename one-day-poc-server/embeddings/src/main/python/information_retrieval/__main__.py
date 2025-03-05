import atexit

from information_retrieval.data_handler import save_data
from information_retrieval.vector_search import vector_store as vs
from information_retrieval.embedding_service import app

if __name__ == '__main__':
    atexit.register(save_data, vs.index, vs.store)
    app.run(host="0.0.0.0", port=7000)