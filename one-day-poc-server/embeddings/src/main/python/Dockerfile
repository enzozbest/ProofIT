FROM python:3.10

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["python3.10", "./embeddings/src/main/python/information_retrieval/__main__.py"]
EXPOSE 7000
