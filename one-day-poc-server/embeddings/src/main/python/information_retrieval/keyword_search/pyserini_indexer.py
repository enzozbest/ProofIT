import json
import os
from pyserini.index.lucene import LuceneIndexer
from pyserini.search.lucene import LuceneSearcher
from information_retrieval.data_handler import LUCENE_INDEX_DIR

JSONL_FILE = "jsonld_docs.jsonl"

def store_jsonld(name:str, data: dict) -> bool:
    """Stores JSON-LD metadata and indexes it with Pyserini."""
    if not isinstance(data, dict):
        return False

    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    existing_docs = []
    if os.path.exists(LUCENE_INDEX_DIR) and os.listdir(LUCENE_INDEX_DIR):
        try:
            searcher = LuceneSearcher(LUCENE_INDEX_DIR)
            searcher.close()
        except Exception as e:
            print(f"Error with existing index: {e}. Creating a new one.")
            for item in os.listdir(LUCENE_INDEX_DIR):
                item_path = os.path.join(LUCENE_INDEX_DIR, item)
                if os.path.isfile(item_path):
                    os.remove(item_path)
                elif os.path.isdir(item_path):
                    import shutil
                    shutil.rmtree(item_path)

    indexer = LuceneIndexer(LUCENE_INDEX_DIR)
    indexer.add_doc_dict({
        "id": name,
        "contents": json.dumps(data),
    })

    indexer.close()

    print(f"Saved document '{name}' to Lucene index at {LUCENE_INDEX_DIR}")
    return True


def keyword_search(query: str, top_k: int = 5):
    """Performs a keyword-based search using Pyserini (BM25 ranking)."""
    if not os.path.exists(LUCENE_INDEX_DIR) or not os.listdir(LUCENE_INDEX_DIR):
        return []

    try:
        searcher = LuceneSearcher(LUCENE_INDEX_DIR)
        hits = searcher.search(query, k=top_k)

        results = []
        for hit in hits:
            results.append(hit.docid)

        return results
    except Exception as e:
        print(f"Error during keyword search: {e}")
        return []
