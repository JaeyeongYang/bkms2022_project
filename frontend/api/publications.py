import os
import tempfile
import subprocess

import numpy as np
import flask
from flask import jsonify, request
from werkzeug.utils import secure_filename


def register_publication_endpoints(app, stores, mod, config):
    store_neo4j = stores["neo4j"]
    store_postgres = stores["postgres"]

    @app.route("/api/publ", methods=["GET"])
    def search_by_title():
        search = request.args.get("search", None)
        return jsonify(store_neo4j.search_by_title(search))

    @app.route("/api/publ/<string:pkey>", methods=["GET"])
    def get_related_publications(pkey):
        data = store_neo4j.get_related_publications(pkey)
        return jsonify(data)

    def is_file_allowed(filename):
        allowed_exts = [".xml"]
        return any(filename.endswith(ext) for ext in allowed_exts)

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

        res = subprocess.run(
            " ".join(args), shell=True, check=True, capture_output=True
        )
        stdout = res.stdout.decode("UTF-8").strip()
        pkeys = [s.strip() for s in stdout.split("\n")]
        return pkeys

    def _make_embed(res):
        pkeys = list(map(lambda x: x["pkey"], res))
        titles = list(map(lambda x: x["title"], res))
        embeds = np.array(mod(titles)).tolist()
        store_postgres.insert_pkeys_embeds(pkeys, embeds)

    def _upload_data(filepath, config):
        pkeys = _parse_and_upload_data(filepath, config)

        store_neo4j.drop_graphs()
        store_neo4j.drop_similar_relationships()

        node_count, rel_count, community_count = store_neo4j.create_community_graph()

        res = store_neo4j.get_titles(pkeys)
        _make_embed(res)

        return {
            "pkeys": pkeys,
            "node_count": node_count,
            "relationship_count": rel_count,
            "community_count": community_count,
        }

    @app.route("/db/init/reset_graph", methods=["GET"])
    def reset_graph():
        store_neo4j.drop_graphs()
        store_neo4j.drop_similar_relationships()

        node_count, rel_count, community_count = store_neo4j.create_community_graph()

        return {
            "node_count": node_count,
            "relationship_count": rel_count,
            "community_count": community_count,
        }

    @app.route("/upload", methods=["GET", "POST"])
    def upload_data_interface():
        if request.method == "POST":
            if "file" not in request.files:
                flask.flash("No file part", "error")
                return flask.redirect(request.url)

            file = request.files["file"]
            if file.filename == "":
                flask.flash("No selected file", "error")
                return flask.redirect(request.url)

            filename = secure_filename(file.filename)
            app.logger.info(filename)
            app.logger.info(is_file_allowed(filename))

            if file and is_file_allowed(filename):
                with tempfile.TemporaryDirectory() as tmpdirname:
                    filepath = os.path.join(tmpdirname, filename)
                    file.save(filepath)

                    res = _upload_data(filepath, config)

                return jsonify({"result": res})

        return flask.render_template("upload.jinja")

    def _upload_data_only(filepath, config):
        pkeys = _parse_and_upload_data(filepath, config)
        # res = store_neo4j.get_titles(pkeys)
        # _make_embed(res)

        return {
            "pkeys": pkeys,
        }

    @app.route("/upload_data_only", methods=["POST"])
    def upload_data_only_interface():
        if "file" not in request.files:
            flask.flash("No file part", "error")
            return flask.redirect(request.url)

        file = request.files["file"]
        if file.filename == "":
            flask.flash("No selected file", "error")
            return flask.redirect(request.url)

        filename = secure_filename(file.filename)
        app.logger.info(filename)
        app.logger.info(is_file_allowed(filename))

        if file and is_file_allowed(filename):
            with tempfile.TemporaryDirectory() as tmpdirname:
                filepath = os.path.join(tmpdirname, filename)
                file.save(filepath)

                res = _upload_data_only(filepath, config)

            return jsonify({"result": res})
