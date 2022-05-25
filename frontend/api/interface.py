import json
import flask
from flask import jsonify, request


def register_interface_endpoints(app, stores):
    store = stores["neo4j"]

    @app.template_filter()
    def numberFormat(value):
        return format(int(value), ",d")

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
