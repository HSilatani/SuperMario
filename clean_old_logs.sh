#!/bin/bash
find ./ -type f -name '*.log' -mmin +180 -exec rm {} \;
