#!/bin/sh

scripts/build.sh || exit 1
kubectl apply -f deploy/minikube || exit 1
kubectl rollout restart deployment scc-backend || exit 1
kubectl rollout restart deployment scc-worker-user-scrub || exit 1
kubectl rollout restart deployment scc-worker-auction-close || exit 1
kubectl rollout restart deployment scc-worker-auction-popularity || exit 1
