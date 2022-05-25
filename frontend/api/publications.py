import json
import flask
from flask import jsonify, request


def register_publication_endpoints(app, stores):
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
        return "." in filename and filename.rsplit(".", 1)[1].lower() in (".xml",)

    @app.route("/upload", methods=["GET", "POST"])
    def upload_data():
        if request.method == "POST":
            pass
        else:
            return flask.render_template("upload.jinja")
