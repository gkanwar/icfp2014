#!/bin/bash

cat $@ | java -cp $BASH_SOURCE/../bin/ laml.compiler.Main
