import os

import flask

from config import load_config
from storage.neo4j_store import Neo4jStore
from api.interface import register_interface_endpoints
from api.publications import register_publication_endpoints


config = load_config()
stores = {
    "neo4j": Neo4jStore(config),
}

app = flask.Flask(__name__, template_folder="templates")
app.secret_key = os.urandom(24)

register_interface_endpoints(app, stores)
register_publication_endpoints(app, stores, config)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=config["FLASK_PORT"])
