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
        f"--host={config['NEO4J_HOST']}",
        f"--user={config['NEO4J_USER']}",
        f"--password={config['NEO4J_PASS']}",
        "--store-all",
        filepath,
        "./data/dblp.dtd",
    ]

    res = subprocess.Popen(
        args=args,
        shell=True,
        stdout=subprocess.PIPE,
    )
    stdout, _ = res.communicate()

    pkeys = [s.strip() for s in bytes(stdout).decode("utf-8").split()]
    return pkeys


def _update_community_graph(config):
    store = Neo4jStore(config)

    store.drop_graphs()
    node_count, rel_count, community_count = store.create_community_graph()

    return node_count, rel_count, community_count


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
