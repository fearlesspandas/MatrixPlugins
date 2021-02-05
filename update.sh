#!/bin/bash

aws s3 cp --r s3://$BUCKET/plugins/ .

ret=$?

if [ ret ];then
  echo "Success"
else
  echo "Failed to migrate"