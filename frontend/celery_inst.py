import subprocess

from celery import Celery

from config import load_config
from storage.neo4j_store import Neo4jStore


config = load_config()
celery_broker_url = "{protocol}://{host}:{port}/{number}".format(
    protocol=config["CELERY_DB_PROTOCOL"],
    host=config["CELERY_DB_HOST"],
    port=config["CELERY_DB_PORT"],
    number=config["CELERY_DB_NUMBER"],
)

app = Celery(
    "worker",
    backend=celery_broker_url,
    broker=celery_broker_url,
)


def _parse_and_upload_data(filepath, config):
    args = [
        "java",
        "-jar",
        "./bin/app.jar",
        "--host",
        config["NEO4J_HOST"],
        "--user",
        config["NEO4J_USER"],
        "--password",
        config["NEO4J_PASS"],
        "--store-all",
        filepath,
        "./bin/dblp.dtd",
    ]

    res = subprocess.run(" ".join(args), shell=True, check=True, capture_output=True)
    stdout = res.stdout.decode("UTF-8").strip()
    pkeys = [s.strip() for s in stdout.split("\n")]
    return pkeys


@app.task(bind=True)
def upload_data(self, filepath, config):
    self.update_state(state="PROGRESS", meta={"message": "Parsing data"})
    pkeys = _parse_and_upload_data(filepath, config)

    self.update_state(state="PROGRESS", meta={"message": "Removing existing graphs"})
    store = Neo4jStore(config)
    store.drop_graphs()

    self.update_state(state="PROGRESS", meta={"message": "Detecting communities"})
    node_count, rel_count, community_count = store.create_community_graph()
    store.close()

    self.update_state(state="PROGRESS", meta={"message": "Generating text embedding"})

    return {
        "message": "Data upload complete ({})".format(pkeys),
        "pkeys": pkeys,
        "node_count": node_count,
        "relationship_count": rel_count,
        "community_count": community_count,
    }
