#!/bin/bash

awk '/MACD/{split($8,arr,",");print $2,arr[9],arr[10],arr[11],arr[14],arr[15],arr[16],arr[17],arr[20],arr[21]}' strategy.log
