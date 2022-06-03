import os
import tempfile

import flask
from flask import jsonify, request
from werkzeug.utils import secure_filename

from celery_inst import upload_data as task_upload_data


def register_publication_endpoints(app, stores, config):
    store = stores["neo4j"]

    @app.route("/api/publ", methods=["GET"])
    def search_by_title():
        search = request.args.get("search", None)
        return jsonify(store.search_by_title(search))

    @app.route("/api/publ/<string:pkey>", methods=["GET"])
    def get_related_publications(pkey):
        data = store.get_related_publications(pkey)
        return jsonify(data)

    def is_file_allowed(filename):
        allowed_exts = [".xml"]
        return any(filename.endswith(ext) for ext in allowed_exts)

    @app.route("/upload", methods=["GET", "POST"])
    def upload_data():
        if request.method == "POST":
            if "file" not in request.files:
                flask.flash("No file part", "error")
                return flask.redirect(request.url)

            file = request.files["file"]
            if file.filename == "":
                flask.flash("No selected file", "error")
                return flask.redirect(request.url)

            filename = secure_filename(file.filename)
            app.logger.info(filename)
            app.logger.info(is_file_allowed(filename))

            if file and is_file_allowed(filename):
                with tempfile.TemporaryDirectory() as tmpdirname:
                    filepath = os.path.join(tmpdirname, filename)
                    file.save(filepath)

                    task_id = task_upload_data.delay(filepath, config)

                return flask.redirect(flask.url_for("upload_data", task_id=task_id))

        task_id = request.args.get("task_id", None)
        return flask.render_template("upload.jinja", task_id=task_id)

    @app.route("/upload/progress", methods=["GET"])
    def upload_data_progress():
        task_id = request.args.get("task_id", None)
        if task_id is None:
            return jsonify({"state": "INVALID"})

        task = task_upload_data.AsyncResult(task_id)
        if task.state == "PENDING":
            resp = {"state": task.state}
        elif task.state == "FAILURE":
            resp = {"state": task.state}
        else:
            resp = {"state": task.state, "info": task.info}

        return jsonify(resp)
