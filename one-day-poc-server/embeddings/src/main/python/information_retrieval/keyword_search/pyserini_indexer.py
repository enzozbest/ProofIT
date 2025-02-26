import json
import os
from pyserini.index.lucene import LuceneIndexer
from pyserini.search.lucene import LuceneSearcher

LUCENE_INDEX_DIR = "jsonld_index"
JSONL_FILE = "jsonld_docs.jsonl"

def store_jsonld(name:str, data: dict) -> bool:
    """Stores JSON-LD metadata and indexes it with Pyserini."""
    if not isinstance(data, dict):
        return False

    os.makedirs(LUCENE_INDEX_DIR, exist_ok=True)

    indexer = LuceneIndexer(LUCENE_INDEX_DIR)
    indexer.add_doc_dict({
        "id": name,
        "contents": json.dumps(data),
    })
    indexer.close()

    return True


def keyword_search(query: str, top_k: int = 5):
    """Performs a keyword-based search using Pyserini (BM25 ranking)."""
    if not os.path.exists(LUCENE_INDEX_DIR) or not os.listdir(LUCENE_INDEX_DIR):
        return []

    searcher = LuceneSearcher(LUCENE_INDEX_DIR)
    hits = searcher.search(query, k=top_k)

    results = []
    for hit in hits:
        results.append(hit.docid)

    return results