#!/bin/bash

if [ $# -eq 0 ]
 then
   FILE_NAME=application.log
 else
   FILE_NAME=$1		
fi

awk '/>>>|MACD|CONFIRM|PRICE_LOGGER - POSITION/{
if(/>>>/)
 {print $0,$1}
else if(/MACD/)
 {
  split($8,arr,",")
  print $2,arr[9],arr[10],arr[11],arr[14],arr[15],arr[16],arr[17],arr[18],arr[19],arr[20],arr[21],arr[22],arr[23],arr[24],arr[25]
 }
else if(/CONFIRM.{7}d/)
 {print $0}
else if(/POSITION/)
 {print $0}
}' $FILE_NAME
