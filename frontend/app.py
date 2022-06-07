import os

import flask
import tensorflow as tf
import tensorflow_hub as hub

from config import load_config
from storage.neo4j_store import Neo4jStore
from storage.postgres_store import PostgresStore
from api.database import register_database_endpoints
from api.interface import register_interface_endpoints
from api.publications import register_publication_endpoints


config = load_config()
stores = {
    "neo4j": Neo4jStore(config),
    "postgres": PostgresStore(config),
}

app = flask.Flask(__name__, template_folder="templates")
app.secret_key = os.urandom(24)

mod = hub.load("./data/universal-sentence-encoder_4")

register_database_endpoints(app, stores, mod)
register_interface_endpoints(app, stores)
register_publication_endpoints(app, stores, mod, config)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=config["FLASK_PORT"])
