#!/bin/bash

aws s3 cp --recursive s3://$BUCKET/matrixplugins/ .

ret=$?

if [ ret ];then
  echo "Success"
else
  echo "Failed to migrate"