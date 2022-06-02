FROM python:3.9.12-bullseye AS builder

ENV PYTHONFAULTHANDLER=1 \
  PYTHONUNBUFFERED=1 \
  PYTHONHASHSEED=random \
  PIP_NO_CACHE_DIR=off \
  PIP_DISABLE_PIP_VERSION_CHECK=on \
  PIP_DEFAULT_TIMEOUT=100 \
  POETRY_VERSION=1.1.13

# System deps:
RUN pip install "poetry==$POETRY_VERSION"

# Copy only requirements to cache them in docker layer
WORKDIR /pysetup
COPY poetry.lock pyproject.toml /pysetup/

# Project initialization:
RUN poetry config virtualenvs.create false \
  && poetry install --no-dev --no-interaction --no-ansi

WORKDIR /code

EXPOSE 8080

FROM builder AS web

ENTRYPOINT ["python", "app.py"]

FROM web AS worker

ENTRYPOINT ["celery", "-A", "worker", "worker", "--loglevel=info"]

