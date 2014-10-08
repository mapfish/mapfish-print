#!/usr/bin/env sh
echo "Current directory is $(pwd)"
echo "\n=== SUREFIRE REPORTS ===\n"

for F in examples/build/test-results/*.xml
do
    echo $F
    cat $F
    echo
done