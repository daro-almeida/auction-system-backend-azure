#!/bin/sh
cd testing/functional
python3 -m pipenv install
python3 -m pipenv run python3 main.py $@
