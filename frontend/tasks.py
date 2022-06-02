import subprocess

from celery import Celery

from config import load_config


config = load_config()
celery_broker_url = "{protocol}://{host}:{port}/{number}".format(
    protocol=config["CELERY_DB_PROTOCOL"],
    host=config["CELERY_DB_HOST"],
    port=config["CELERY_DB_PORT"],
    number=config["CELERY_DB_NUMBER"],
)

app = Celery(
    "worker",
    backend=celery_broker_url,
    broker=celery_broker_url,
)

@app.task
def parse_and_upload_to_neo4j(filepath, config):

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

    res = subprocess.Popen(
        args=" ".join(args),
        shell=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    pkeys = [
        s.strip() for s in bytes(res.stdout.read()).decode("utf-8").split()
    ]

    return {'pkeys': pkeys}

