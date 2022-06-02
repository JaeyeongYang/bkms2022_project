from config import load_config
from celery import Celery


if __name__ == "__main__":
    config = load_config()
    celery_broker_url = "{protocol}://{host}:{port}/{number}".format(
        protocol=config["CELERY_DB_PROTOCOL"],
        host=config["CELERY_DB_HOST"],
        port=config["CELERY_DB_PORT"],
        number=config["CELERY_DB_NUMBER"],
    )

    celery = Celery(
        "worker",
        backend=celery_broker_url,
        broker=celery_broker_url,
    )
    celery.autodiscover_tasks()
