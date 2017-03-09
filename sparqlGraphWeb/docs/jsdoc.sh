#!/bin/bash


cd $1

head -18 index.html > temp.html
cat main.html >> temp.html
tail -18 index.html >> temp.html
mv index.html index_backup.html
mv temp.html index.html

