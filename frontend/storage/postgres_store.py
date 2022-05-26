import psycopg2


class PostgresStore:
    def __init__(self, config):
        self.host = config["POSTGRES_HOST"]
        self.user = config["POSTGRES_USER"]
        self.password = config["POSTGRES_PASS"]
        self.dbname = config["POSTGRES_DB"]
        self.port = config["POSTGRES_PORT"]

    def get_db_conn(self):
        return psycopg2.connect(
            host=self.host,
            dbname=self.dbname,
            user=self.user,
            password=self.password,
            port=self.port,
        )
