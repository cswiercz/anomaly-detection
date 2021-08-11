#!/bin/bash

#set -x

ONE=${@: 1:1}
TWO=${@: 2:1}
ARGS=${@: 3}

DATA_DIR=$ONE/data
OUTPUT_DIR=$ONE/$TWO
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "creating directory: $OUTPUT_DIR..."
    mkdir $OUTPUT_DIR
fi

echo "data directory:   $DATA_DIR"
echo "output directory: $OUTPUT_DIR"

DATA_FILES=$(find $DATA_DIR -name '*.csv')

echo "running on all files:"
echo
for FILE in $DATA_FILES
do
    BASENAME=$(basename $FILE)
    echo "copying $FILE to ~/datasets/input.csv ..."
    cp $FILE ~/datasets/input.csv

    ./gradlew test --tests com.amazon.randomcutforest.ERCF.ErcfExperimentTests

    echo "copying ~/datasets/output.csv to $OUTPUT_DIR/results_$BASENAME ..."
    cp ~/datasets/output.csv $OUTPUT_DIR/results_$BASENAME
done
