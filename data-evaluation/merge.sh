#!/bin/sh

python3 data-extract.py --force --label 1 -k accel data/raw/walking-00-60s.csv data/labeled/walking-acc-00-60s.csv
python3 data-extract.py --force --label 1 -k accel data/raw/walking-01-60s.csv data/labeled/walking-acc-01-60s.csv

python3 data-extract.py --force --label 2 -k accel data/raw/running-00-60s.csv data/labeled/running-acc-00-60s.csv
python3 data-extract.py --force --label 2 -k accel data/raw/running-01-60s.csv data/labeled/running-acc-01-60s.csv

python3 data-extract.py --force --label 3 -k accel data/raw/jumping-00-60s.csv data/labeled/jumping-acc-00-60s.csv
python3 data-extract.py --force --label 3 -k accel data/raw/jumping-01-60s.csv data/labeled/jumping-acc-01-60s.csv

python3 data-extract.py --force --label 4 -k accel data/raw/squatting-00-60s.csv data/labeled/squatting-acc-00-60s.csv
python3 data-extract.py --force --label 4 -k accel data/raw/squatting-01-60s.csv data/labeled/squatting-acc-01-60s.csv

python3 data-extract.py --force --label 5 -k accel data/raw/standing-00-60s.csv data/labeled/standing-acc-00-60s.csv
python3 data-extract.py --force --label 5 -k accel data/raw/standing-01-60s.csv data/labeled/standing-acc-01-60s.csv 

python3 data-extract.py --force --label 6 -k accel data/raw/sitting-00-60s.csv data/labeled/sitting-acc-00-60s.csv 
python3 data-extract.py --force --label 6 -k accel data/raw/sitting-01-60s.csv data/labeled/sitting-acc-01-60s.csv 
