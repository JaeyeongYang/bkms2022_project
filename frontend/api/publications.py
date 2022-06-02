import os
import asyncio
from pathlib import Path
import subprocess
import tempfile

import flask
from flask import jsonify, request
from werkzeug.utils import secure_filename


def register_publication_endpoints(app, stores, config):
    store = stores["neo4j"]

    @app.route("/api/publ", methods=["GET"])
    def search_by_title():
        search = request.args.get("search", None)
        return jsonify(store.search_by_title(search))

    def serialize_related_publications(data):
        nodes = []
        edges = []
        for record in data:
            p = record["p"]
            ps = record["ps"]
            s = record["s"]

            p["label"] = p["key"]
            ps["label"] = ps["key"]
            s["label"] = s["key"]

            nodes.append(p)
            nodes.append(ps)
            nodes.append(s)
            edges.append({"source": p, "target": s})
            edges.append({"source": ps, "target": s})

        return {"nodes": nodes, "edges": edges}

    @app.route("/api/publ/<string:pkey>", methods=["GET"])
    def get_related_publications(pkey):
        data = store.get_related_publications(pkey)
        # return jsonify(serialize_related_publications(data))
        return jsonify(data)

    def is_file_allowed(filename):
        allowed_exts = [".xml"]
        return any(filename.endswith(ext) for ext in allowed_exts)

    async def parse_and_upload_to_neo4j(filepath):
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

        app.logger.info(args)
        res = subprocess.Popen(
            args=" ".join(args),
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
        return [s.strip() for s in bytes(res.stdout.read()).decode("utf-8").split()]

    async def set_up_graph_recommendation():
        pass

    async def save_text_embeddings(pkeys):
        pass

    @app.route("/upload", methods=["GET", "POST"])
    async def upload_data():
        if request.method == "POST":
            app.logger.info(list(request.files.keys()))
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
                with tempfile.NamedTemporaryFile(suffix=".xml") as f:
                    file.save(f)
                    pkeys = await parse_and_upload_to_neo4j(f.name)

                # return flask.redirect(flask.url_for("download_file", name=filename))
                # return flask.redirect(request.url)
                flask.flash(
                    f"{len(pkeys)} publication{'s' if len(pkeys) > 1 else ''} uploaded.",
                    "info",
                )
                return flask.redirect(request.url)
                # return flask.render_template_string(
                #     "<pre>\n{{ res }}\n</pre>", res=pkeys
                # )

        return flask.render_template("upload.jinja")
