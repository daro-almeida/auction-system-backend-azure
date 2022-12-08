#!/bin/sh
#arg_example: workload1.yml
docker build -t git.d464.sh/diogo464/scc-tester:latest -f deploy/dockerfiles/tester.dockerfile --build-arg yml=$@ ./tester
docker push git.d464.sh/diogo464/scc-tester:latest