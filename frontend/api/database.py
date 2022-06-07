import json

import numpy as np
import flask
from flask import jsonify, request

from storage.neo4j_store import Neo4jStore
from storage.postgres_store import PostgresStore


def register_database_endpoints(app, stores, mod):
    store_neo4j: Neo4jStore = stores["neo4j"]
    store_postgres: PostgresStore = stores["postgres"]

    @app.route("/db/init/postgres")
    def create_postgres_tables():
        store_postgres.create_embed_table()
        return jsonify({"state": "SUCCESS"})

    @app.route("/db/init/embed")
    def generate_all_embeds():
        num_publ = store_neo4j.get_num_publications_with_title()
        app.logger.info(f"Total {num_publ} publications with title")
        batchsize = 500

        for i in range(0, num_publ, batchsize):
            app.logger.info((i, i + batchsize))
            result = store_neo4j.get_pkeys_and_titles_of(i, i + batchsize)

            pkeys = list(map(lambda x: x["pkey"], result))
            titles = list(map(lambda x: x["title"], result))
            embeds = np.array(mod(titles)).tolist()

            store_postgres.insert_pkeys_embeds(pkeys, embeds)

        return jsonify({"state": "SUCCESS"})