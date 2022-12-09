#!/bin/sh

EXTRA=""
if [ "$TESTID" != "" ]; then
  EXTRA="-$TESTID"
fi

if [ "$TARGET" == "" ]; then
    echo "TARGET not set"
    exit 1
fi

wget https://static.d464.sh/scc/users.data || exit 1
/home/node/artillery/bin/run run -t $TARGET --output workload1.json workload1.yml || exit 1
wget --post-file workload1.json "https://scc.d464.sh/$(date +%y-%m-%d-%R:%S)_workload1$EXTRA.json" || exit 1