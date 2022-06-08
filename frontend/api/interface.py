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

            authors = ", ".join(author_names)
            if authors == "":
                authors = "[NO AUTHOR INFO]"

            ret.append({"p": record["p"], "authors": authors})
        return ret

    @app.route("/search", methods=["GET"])
    def search():
        query = request.args.get("q", None)
        if query is None:
            return flask.redirect(flask.url_for("index"))

        page = int(request.args.get("page", 1))
        limit = int(request.args.get("limit", 10))

        res = store_neo4j.search_by_title(query, page, limit)
        print(res["data"])
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

    @app.route("/recommend", methods=["GET"])
    def get_recommendations_interface():
        pkey = request.args.get("pkey", None)
        if pkey is None:
            return flask.redirect(flask.url_for("index"))

        k = request.args.get("k", 25)

        # Retrieve data for the target publication
        res_target = store_neo4j.search_by_pkey([pkey])
        data_target = serialize_search_data(res_target)[0]
        embed_target = store_postgres.retrieve_embeds(pkey)[0][1]

        # Generate candidates
        res_cand = store_neo4j.generate_candidates(pkey, k)
        dict_cand = {x["pkey"]: x["node_similarity"] for x in res_cand}

        # Retrieve sentence embeddings
        pkeys = list(map(lambda x: x["pkey"], res_cand))
        res_embeds = store_postgres.retrieve_embeds(pkeys)
        dict_embeds = {x[0]: x[1] for x in res_embeds}

        # Prepare paper information
        res_recom = store_neo4j.search_by_pkey(pkeys)
        data_recom = serialize_search_data(res_recom)

        # Calculate content similarity
        for i in range(len(data_recom)):
            _pkey = data_recom[i]["p"]["key"]
            _embed = dict_embeds.get(_pkey, None)

            if _embed is not None:
                data_recom[i]["content_similarity"] = cosine_sim(embed_target, _embed)
                data_recom[i]["node_similarity"] = dict_cand[_pkey]

        # Filter and re-sort data
        data_recom = list(filter(lambda x: "content_similarity" in x, data_recom))
        data_recom.sort(key=lambda x: -x["content_similarity"])

        return flask.render_template(
            "recommend.jinja",
            pkey=pkey,
            data_target=data_target,
            data_recom=data_recom,
        )
