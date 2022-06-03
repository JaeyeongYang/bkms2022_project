FROM python:3.9.12-bullseye

ENV PYTHONFAULTHANDLER=1 \
	PYTHONUNBUFFERED=1 \
	PYTHONHASHSEED=random \
	PIP_NO_CACHE_DIR=off \
	PIP_DISABLE_PIP_VERSION_CHECK=on \
	PIP_DEFAULT_TIMEOUT=100 \
	POETRY_VERSION=1.1.13 \
	DOCKERIZE_VERSION=v0.6.1 \
	JAVA_VERSION=18.0.1.1 \
	JAVA_HOME=/usr/local/openjdk-18

ENV PATH $JAVA_HOME/bin:$PATH

RUN set -eux; \
	apt-get update; \
	apt-get install -y --no-install-recommends \
	# utilities for keeping Debian and OpenJDK CA certificates in sync
	ca-certificates p11-kit ruby \
	;

# Install Java 18
RUN set -eux; \
	\
	arch="$(dpkg --print-architecture)"; \
	case "$arch" in \
	'amd64') \
	downloadUrl='https://download.java.net/java/GA/jdk18.0.1.1/65ae32619e2f40f3a9af3af1851d6e19/2/GPL/openjdk-18.0.1.1_linux-x64_bin.tar.gz'; \
	downloadSha256='4f81af7203fa4c8a12c9c53c94304aab69ea1551bc6119189c9883f4266a2b24'; \
	;; \
	'arm64') \
	downloadUrl='https://download.java.net/java/GA/jdk18.0.1.1/65ae32619e2f40f3a9af3af1851d6e19/2/GPL/openjdk-18.0.1.1_linux-aarch64_bin.tar.gz'; \
	downloadSha256='e667166c47e90874af3641ad4a3952d3c835627e4301fa1f0d620d9b6e366519'; \
	;; \
	*) echo >&2 "error: unsupported architecture: '$arch'"; exit 1 ;; \
	esac; \
	\
	savedAptMark="$(apt-mark showmanual)"; \
	apt-get update; \
	apt-get install -y --no-install-recommends \
	wget \
	; \
	rm -rf /var/lib/apt/lists/*; \
	\
	wget --progress=dot:giga -O openjdk.tgz "$downloadUrl"; \
	echo "$downloadSha256 *openjdk.tgz" | sha256sum --strict --check -; \
	\
	mkdir -p "$JAVA_HOME"; \
	tar --extract \
	--file openjdk.tgz \
	--directory "$JAVA_HOME" \
	--strip-components 1 \
	--no-same-owner \
	; \
	rm openjdk.tgz*; \
	\
	apt-mark auto '.*' > /dev/null; \
	[ -z "$savedAptMark" ] || apt-mark manual $savedAptMark > /dev/null; \
	apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false; \
	\
	# update "cacerts" bundle to use Debian's CA certificates (and make sure it stays up-to-date with changes to Debian's store)
	# see https://github.com/docker-library/openjdk/issues/327
	#     http://rabexc.org/posts/certificates-not-working-java#comment-4099504075
	#     https://salsa.debian.org/java-team/ca-certificates-java/blob/3e51a84e9104823319abeb31f880580e46f45a98/debian/jks-keystore.hook.in
	#     https://git.alpinelinux.org/aports/tree/community/java-cacerts/APKBUILD?id=761af65f38b4570093461e6546dcf6b179d2b624#n29
	{ \
	echo '#!/usr/bin/env bash'; \
	echo 'set -Eeuo pipefail'; \
	echo 'trust extract --overwrite --format=java-cacerts --filter=ca-anchors --purpose=server-auth "$JAVA_HOME/lib/security/cacerts"'; \
	} > /etc/ca-certificates/update.d/docker-openjdk; \
	chmod +x /etc/ca-certificates/update.d/docker-openjdk; \
	/etc/ca-certificates/update.d/docker-openjdk; \
	\
	# https://github.com/docker-library/openjdk/issues/331#issuecomment-498834472
	find "$JAVA_HOME/lib" -name '*.so' -exec dirname '{}' ';' | sort -u > /etc/ld.so.conf.d/docker-openjdk.conf; \
	ldconfig; \
	\
	# https://github.com/docker-library/openjdk/issues/212#issuecomment-420979840
	# https://openjdk.java.net/jeps/341
	java -Xshare:dump; \
	\
	# basic smoke test
	fileEncoding="$(echo 'System.out.println(System.getProperty("file.encoding"))' | jshell -s -)"; [ "$fileEncoding" = 'UTF-8' ]; rm -rf ~/.java; \
	javac --version; \
	java --version

# Install google-protobuf
RUN gem install google-protobuf

# System deps:
RUN pip install "poetry==$POETRY_VERSION"

# Install dockerize:
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
	&& tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
	&& rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz

# Copy only requirements to cache them in docker layer
WORKDIR /pysetup
COPY poetry.lock pyproject.toml /pysetup/

# Project initialization:
RUN poetry config virtualenvs.create false \
	&& poetry install --no-dev --no-interaction --no-ansi

WORKDIR /code

EXPOSE 8080

