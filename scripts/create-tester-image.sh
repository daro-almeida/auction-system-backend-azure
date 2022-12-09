#!/bin/sh
docker build -t git.d464.sh/diogo464/scc-tester:latest -f deploy/dockerfiles/tester.dockerfile . || exit 1
docker push git.d464.sh/diogo464/scc-tester:latest