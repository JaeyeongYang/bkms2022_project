from neo4j import GraphDatabase


class Neo4jStore:
    __slot__ = ("driver",)

    def __init__(self, config):
        self.driver = GraphDatabase.driver(
            config["NEO4J_HOST"],
            auth=(config["NEO4J_USER"], config["NEO4J_PASS"]),
        )

    def close(self):
        self.driver.close()

    def get_num_publications(self):
        with self.driver.session() as session:
            result = session.run("MATCH (p:Publication) RETURN COUNT(p)")
            return result.single()[0]

    def get_num_publications_with_title(self):
        with self.driver.session() as session:
            result = session.run(
                "MATCH (p:Publication) WHERE p.title <> '' RETURN COUNT(p)"
            )
            return result.single()[0]

    def get_num_authors(self):
        with self.driver.session() as session:
            result = session.run("MATCH (a:Author) RETURN COUNT(a)")
            return result.single()[0]

    def get_num_streams(self):
        with self.driver.session() as session:
            result = session.run("MATCH (s:Stream) RETURN COUNT(s)")
            return result.single()[0]

    def get_pkeys_and_titles_of(self, start, end):
        with self.driver.session() as session:
            result = session.run(
                """
                MATCH (p:Publication)
                WHERE p.title <> ''
                RETURN p.key AS pkey, p.title as title
                ORDER BY pkey
                SKIP $start
                LIMIT $num
                """,
                start=start,
                num=end - start,
            )
            return [record.data() for record in result]

    def get_titles(self, pkeys):
        with self.driver.session() as session:
            result = session.run(
                """
                MATCH (p:Publication)
                WHERE p.key IN $pkeys
                RETURN p.key AS pkey, p.title as title
                ORDER BY pkey
                """,
                pkeys=pkeys,
            )
            return [record.data() for record in result]

    def search_by_pkey(self, pkeys):
        with self.driver.session() as session:
            result = session.run(
                f"""
                MATCH (p:Publication)
                WHERE p.key IN $pkeys
                WITH p
                MATCH (p)-[:AUTHORED_BY]->(a:Author)
                WITH p, COLLECT(a) AS authors
                RETURN p, authors
                """,
                pkeys=pkeys,
            )
            return [record.data() for record in result]

    def search_by_title(self, search=None, page=1, limit=24):
        query_with = r"""
        WITH REDUCE(res = [], w IN SPLIT($search, " ") |
            CASE WHEN w <> '' THEN res + (".*" + w + ".*")
            ELSE res END) AS res
        """
        query_where = "ALL(regexp IN res WHERE p.title =~ regexp)"

        if not isinstance(page, int):
            raise RuntimeError('"page" is not an integer.')
        elif page <= 0:
            raise RuntimeError('"page" should be a positive integer.')

        query_skip = f"SKIP {(page - 1) * limit}" if page > 1 else ""

        with self.driver.session() as session:
            result = session.run(
                f"""
                {query_with}
                MATCH (p:Publication)
                WHERE {query_where}
                RETURN COUNT(p)
                """,
                search=search,
            )
            count = result.single()[0]
            if count == 0:
                return {"count": count, "data": []}

        with self.driver.session() as session:
            result = session.run(
                f"""
                {query_with}
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
                search=search,
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

    def has_community_graph(self):
        with self.driver.session() as session:
            result = session.run(
                "CALL gds.graph.list() YIELD graphName "
                "WHERE graphName = 'bib_community' "
                "RETURN COUNT(*)"
            )
            return result.single()[0] == 1

    def drop_graphs(self):
        with self.driver.session() as session:
            # Drop bib_community graph if exists
            result = session.run("CALL gds.graph.list() YIELD graphName")

            for (graph_name,) in result.values():
                _ = session.run(
                    "CALL gds.graph.drop($graph_name, False)", graph_name=graph_name
                )

    def create_community_graph(self):
        with self.driver.session() as session:
            # Create bib_community graph
            result = session.run(
                """
                CALL gds.graph.project(
                    'bib_community',
                    ['Author', 'Publication', 'Stream'],
                    {
                        AUTHORED_BY: {
                            orientation: 'UNDIRECTED'
                        },
                        CITED_BY: {
                            orientation: 'NATURAL'
                        },
                        GROUPED_BY: {
                            orientation: 'UNDIRECTED'
                        }
                    }
                )
                YIELD nodeCount, relationshipCount
                """
            )
            node_count, rel_count = result.single()

            # Run Louvain community detection algorithm
            result = session.run(
                "CALL gds.louvain.write('bib_community', {writeProperty: 'community_id'}) "
                "YIELD communityCount"
            )

            community_count = result.single()[0]

            return node_count, rel_count, community_count

    def generate_candidates(self, pkey, k):
        with self.driver.session() as session:
            # Retrieve community ID
            result = session.run(
                "MATCH (p:Publication {key: $pkey}) RETURN p.community_id",
                pkey=pkey,
            )
            cid = result.single()[0]
            sim_graph_name = f"sim_graph_{cid}"

            # Check if similiarity graph exists
            result = session.run(
                """
                CALL gds.graph.exists($graph_name)
                YIELD exists
                RETURN exists
                """,
                graph_name=sim_graph_name,
            )
            graph_exists = result.single()[0]

            # Generate similarity graph and SIMILAR relationships
            if not graph_exists:
                # Generate similarity graph for the given community id
                _ = session.run(
                    f"""
                    CALL gds.graph.project.cypher(
                        "{sim_graph_name}",
                        "MATCH (n) WHERE n.community_id = {cid} RETURN id(n) AS id, labels(n) AS labels",
                        "MATCH (n)-[r:CITED_BY|AUTHORED_BY|GROUPED_BY]-(m) WHERE n.community_id = {cid} AND m.community_id = {cid} RETURN id(n) AS source, id(m) AS target, type(r) AS type",
                        {{
                            validateRelationships: False
                        }}
                    )
                    YIELD graphName AS graph, nodeQuery, nodeCount AS nodes, relationshipCount AS rels
                    """,
                )

                # Create SIMILAR relationships with similarity scores
                _ = session.run(
                    """
                    CALL gds.nodeSimilarity.write(
                        $graph_name,
                        {
                            writeRelationshipType: 'SIMILAR',
                            writeProperty: 'score'
                        }
                    )
                    YIELD nodesCompared, relationshipsWritten
                    """,
                    graph_name=sim_graph_name,
                )

            # Find top K candidates based on the similarity scores
            result = session.run(
                """
                MATCH (p1:Publication {key: $pkey, community_id: $cid})-[r:SIMILAR]->(p2:Publication {community_id: $cid}) 
                RETURN p2.key AS pkey, r.score AS node_similarity
                ORDER BY r.score DESC 
                LIMIT $k
                """,
                pkey=pkey,
                cid=cid,
                k=k,
            )

            return [record.data() for record in result]
