#!/bin/sh

mvn compile package || exit 1
podman build -t git.d464.sh/diogo464/scc-backend -f deploy/dockerfiles/backend.dockerfile . || exit 1
podman build -t git.d464.sh/diogo464/scc-worker -f deploy/dockerfiles/worker.dockerfile . || exit 1
podman push --tls-verify=false git.d464.sh/diogo464/scc-backend || exit 1
podman push --tls-verify=false git.d464.sh/diogo464/scc-worker || exit 1
