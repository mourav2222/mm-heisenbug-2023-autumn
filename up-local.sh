#!/bin/bash

docker stop $(docker ps -a --filter "name=rococo" -q)
docker rm $(docker ps -a --filter "name=rococo" -q)

docker run --name rococo-db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -d mysql:8.3.0

cd ./rococo-client
npm i
npm run dev
