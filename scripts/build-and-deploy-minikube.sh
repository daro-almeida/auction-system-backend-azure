#!/bin/sh

mvn compile package || exit 1
podman build -t git.d464.sh/diogo464/scc-backend-app -f deploy/dockerfiles/backend-app.dockerfile . || exit 1
podman build -t git.d464.sh/diogo464/scc-worker -f deploy/dockerfiles/worker.dockerfile . || exit 1
podman push --tls-verify=false git.d464.sh/diogo464/scc-backend-app || exit 1
podman push --tls-verify=false git.d464.sh/diogo464/scc-worker || exit 1
kubectl apply -f deploy/minikube || exit 1
kubectl rollout restart deployment scc-backend-app || exit 1
kubectl rollout restart deployment scc-worker-user-scrub || exit 1
kubectl rollout restart deployment scc-worker-auction-close || exit 1
kubectl rollout restart deployment scc-worker-auction-popularity || exit 1