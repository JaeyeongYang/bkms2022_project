from neo4j import GraphDatabase


class Neo4jStore:
    __slot__ = ("driver",)

    def __init__(self, config):
        self.driver = GraphDatabase.driver(
            config["NEO4J_HOST"],
            auth=(
                config["NEO4J_USER"],
                config["NEO4J_PASS"],
            ),
        )

    def get_num_publications(self):
        with self.driver.session() as session:
            result = session.run(f"MATCH (p:Publication) RETURN COUNT(p)")
            return result.single()[0]

    def get_num_authors(self):
        with self.driver.session() as session:
            result = session.run(f"MATCH (a:Author) RETURN COUNT(a)")
            return result.single()[0]

    def get_num_streams(self):
        with self.driver.session() as session:
            result = session.run(f"MATCH (s:Stream) RETURN COUNT(s)")
            return result.single()[0]

    def search_by_title(self, query=None, page=None, limit=24):
        conds_where = []
        conds_where.append("p.title IS NOT NULL")
        if query is not None:
            conds_where.append("p.title CONTAINS $title")
        query_where = " AND ".join(conds_where)

        if page is None:
            page = 1
        query_skip = f"SKIP {(page - 1) * limit}" if page > 1 else ""

        with self.driver.session() as session:
            result = session.run(
                f"MATCH (p:Publication) WHERE {query_where} RETURN COUNT(p)",
                title=query,
            )
            count = result.single()[0]
            if count == 0:
                return {"count": count, "data": []}

        with self.driver.session() as session:
            result = session.run(
                f"""
                MATCH (p:Publication)
                WHERE {query_where}
                WITH p
                MATCH (p)-[:AUTHORED_BY]->(a:Author)
                WITH p, COLLECT(a) AS authors
                RETURN p, authors
                ORDER BY p.year DESC
                {query_skip}
                LIMIT $limit
                """,
                title=query,
                limit=limit,
            )
            return {"count": count, "data": [record.data() for record in result]}

    def get_related_publications(self, pkey):
        with self.driver.session() as session:
            result = session.run(
                """
                MATCH (p:Publication {key: $pkey})
                MATCH (p)-[]->(s:Stream)<-[]-(ps:Publication)
                RETURN *
                """,
                pkey=pkey,
            )
            return [record.data() for record in result]
