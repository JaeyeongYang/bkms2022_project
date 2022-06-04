import psycopg2


class PostgresStore:
    def __init__(self, config):
        self.host = config["POSTGRES_HOST"]
        self.user = config["POSTGRES_USER"]
        self.password = config["POSTGRES_PASS"]
        self.dbname = config["POSTGRES_DB"]
        self.port = config["POSTGRES_PORT"]

        self.conn = self.get_db_conn()

    def __del__(self):
        self.close()

    def get_db_conn(self):
        return psycopg2.connect(
            host=self.host,
            dbname=self.dbname,
            user=self.user,
            password=self.password,
            port=self.port,
        )

    def close(self):
        self.conn.close()

    def create_embed_table(self):
        cur = self.conn.cursor()
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS embeds (
                pkey TEXT PRIMARY KEY UNIQUE,
                embed FLOAT8[512]
            );
            """
        )
        self.conn.commit()
        cur.close()

    def insert_pkeys_embeds(self, pkeys, embeds):
        cur = self.conn.cursor()
        data = list(zip(pkeys, embeds))

        cur.executemany(
            """
            INSERT INTO embeds
            VALUES (%s, %s)
            ON CONFLICT (pkey)
            DO UPDATE SET embed = excluded.embed;
            """,
            data,
        )

        self.conn.commit()
        cur.close()

    def retrieve_embeds(self, pkeys):
        cur = self.conn.cursor()
        if isinstance(pkeys, list):
            cur.execute(
                """
                SELECT pkey, embed
                FROM embeds
                WHERE pkey IN %s
                ORDER BY pkey
                """,
                (tuple(pkeys),),
            )
        else:
            cur.execute(
                """
                SELECT pkey, embed
                FROM embeds
                WHERE pkey = %s
                ORDER BY pkey
                """,
                (pkeys,),
            )

        rows = cur.fetchall()

        self.conn.commit()
        cur.close()

        return rows
