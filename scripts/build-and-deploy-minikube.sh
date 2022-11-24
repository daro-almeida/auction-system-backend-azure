#!/bin/sh

mvn compile package || exit 1
podman build -t localhost:5000/scc-backend-app -f deploy/dockerfiles/backend-app.dockerfile . || exit 1
podman push --tls-verify=false localhost:5000/scc-backend-app || exit 1
kubectl apply -f deploy/minikube || exit 1
kubectl rollout restart deployment scc-backend-app || exit 1