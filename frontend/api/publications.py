import json
import flask
from flask import jsonify, request


def register_publication_endpoints(app, stores):
    store = stores["neo4j"]

    @app.template_filter()
    def numberFormat(value):
        return format(int(value), ",d")

    @app.route("/publ/search", methods=["GET"])
    def search_by_title():
        title = request.args.get("title", None)
        return jsonify(store.search_by_title(title))

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

    @app.route("/publ/related", methods=["GET"])
    def get_related_publications():
        pkey = request.args.get("pkey", None)
        if pkey:
            data = store.get_related_publications(pkey)
            # return jsonify(serialize_related_publications(data))
            return jsonify(data)
        else:
            return jsonify([])

    @app.route("/")
    @app.route("/index")
    def index():
        n_publs = store.get_num_publications()
        n_authors = store.get_num_authors()
        n_streams = store.get_num_streams()

        return flask.render_template(
            "index.jinja",
            n_publs=n_publs,
            n_authors=n_authors,
            n_streams=n_streams,
        )

    def serialize_search_data(data):
        ret = []
        for record in data:
            author_names = [a["name"] for a in record["authors"] if a["name"]]
            if len(author_names) > 1:
                author_names[-1] = "& " + author_names[-1]

            ret.append(
                {
                    "p": record["p"],
                    "authors": ", ".join(author_names),
                }
            )
        return ret

    @app.route("/search", methods=["GET"])
    def search():
        query = request.args.get("q", None)
        if query is None:
            return flask.redirect(flask.url_for("index"))

        page = int(request.args.get("page", 1))
        limit = int(request.args.get("limit", 10))

        res = store.search_by_title(query, page, limit)
        count = res["count"]
        data = serialize_search_data(res["data"])

        return flask.render_template(
            "search.jinja",
            query=query,
            page=page,
            limit=limit,
            count=count,
            data=data,
        )
