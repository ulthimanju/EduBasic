#!/bin/sh
set -e

# Detect GID of the docker socket
if [ -S /var/run/docker.sock ]; then
    DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)
    echo "Detected Docker socket GID: $DOCKER_GID"
    
    # Create or update the docker group to match the host
    if getent group "$DOCKER_GID"; then
        GROUP_NAME=$(getent group "$DOCKER_GID" | cut -d: -f1)
        echo "Group with GID $DOCKER_GID already exists: $GROUP_NAME"
        addgroup appuser "$GROUP_NAME"
    else
        echo "Creating group 'docker' with GID $DOCKER_GID"
        addgroup -g "$DOCKER_GID" docker
        addgroup appuser docker
    fi
fi

# Execute the application as appuser
echo "Starting application as appuser..."
exec su-exec appuser java $JAVA_OPTS -jar app.jar
