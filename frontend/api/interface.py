import numpy as np
import flask
from flask import jsonify, request


def register_interface_endpoints(app, stores):
    store_neo4j = stores["neo4j"]
    store_postgres = stores["postgres"]

    @app.template_filter()
    def numberFormat(value):
        return format(int(value), ",d")

    @app.template_filter()
    def simFormat(value):
        return format(value, ".3f")

    @app.route("/")
    @app.route("/index")
    def index():
        n_publs = store_neo4j.get_num_publications()
        n_authors = store_neo4j.get_num_authors()
        n_streams = store_neo4j.get_num_streams()

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

        res = store_neo4j.search_by_title(query, page, limit)
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

    def cosine_sim(x, y):
        return np.dot(x, y) / (np.linalg.norm(x) * np.linalg.norm(y))

    def get_recommendations(pkey):
        embed_origin = store_postgres.retrieve_embeds(pkey)[0][1]

        k = request.args.get("k", 25)
        res = store_neo4j.generate_candidates(pkey, k)
        res.sort(key=lambda x: x["pkey"])

        pkeys = list(map(lambda x: x["pkey"], res))
        embeds = store_postgres.retrieve_embeds(pkeys)
        embeds.sort(key=lambda x: x[0])

        for i in range(len(res)):
            res[i]["content_similarity"] = cosine_sim(embed_origin, embeds[i][1])

        res.sort(key=lambda x: -x["content_similarity"])

        return res

    @app.route("/recommend", methods=["GET"])
    def get_recommendations_interface():
        pkey = request.args.get("pkey", None)
        if pkey is None:
            return flask.redirect(flask.url_for("index"))

        res_orig = store_neo4j.search_by_pkey([pkey])
        data_orig = serialize_search_data(res_orig)

        res_rec = get_recommendations(pkey)
        dict_rec = {r["pkey"]: r for r in res_rec}
        res = store_neo4j.search_by_pkey(list(map(lambda x: x["pkey"], res_rec)))
        data = serialize_search_data(res)
        for i in range(len(data)):
            _pkey = data[i]["p"]["key"]
            data[i]["node_similarity"] = dict_rec[_pkey]["node_similarity"]
            data[i]["content_similarity"] = dict_rec[_pkey]["content_similarity"]
        data.sort(key=lambda x: -x["content_similarity"])

        return flask.render_template(
            "recommend.jinja",
            data_origin=data_orig[0],
            data=data,
        )
