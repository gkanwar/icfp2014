#!/bin/bash

# Helper script to read series of files to compile from given BUILD file
# and run compile.sh with those files as inputs.

lines=`cat $1 | wc -l`
echo "Compiling $lines files..." 1>&2;

tools_dir=`dirname $BASH_SOURCE`
cat $1 | xargs -n $lines $tools_dir/compile.sh
