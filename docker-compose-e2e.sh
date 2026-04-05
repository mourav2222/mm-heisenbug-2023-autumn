#!/bin/bash
source ./docker.properties
export PROFILE="${PROFILE:=docker}"

PROFILE="${PROFILE}" docker compose down

docker_containers="$(docker ps -a --filter "name=rococo" -q)"
docker_images="$(docker images --format '{{.Repository}}:{{.Tag}}' | grep 'rococo')"

if [ ! -z "$docker_containers" ]; then
  echo "### Stop containers: $docker_containers ###"
  docker stop $(docker ps -a --filter "name=rococo" -q)
  docker rm $(docker ps -a --filter "name=rococo" -q)
fi
if [ ! -z "$docker_images" ]; then
  echo "### Remove images: $docker_images ###"
  docker rmi $(docker images --format '{{.Repository}}:{{.Tag}}' | grep 'rococo')
fi

echo "### Build backend images ###"
bash ./gradlew jibDockerBuild -x :rococo-e2e:test
cd rococo-client || exit 1
echo "### Build frontend image ###"
docker build --build-arg PROFILE=${PROFILE} -t dtuchs/rococo-client-${PROFILE}:${FRONT_VERSION} -t dtuchs/rococo-client-${PROFILE}:latest .
cd ../

#cd rococo-e2e || exit 1
echo "### Build e2e tests image ###"
docker build -f rococo-e2e/Dockerfile --build-arg PROFILE=${PROFILE} -t dtuchs/rococo-e2e-${PROFILE}:${FRONT_VERSION} -t dtuchs/rococo-e2e-${PROFILE}:latest .

echo "### List rococo images ###"
docker images | grep rococo

echo "### Deploy (docker compose) rococo application with e2e tests ###"
ARCH=$( [ "$(uname -m)" = "x86_64" ] && echo "amd64" || echo "arm64" )
echo "ARCH: ${ARCH}"

PROFILE="${PROFILE}" ARCH="${ARCH}" \
ALLURE_DOCKER_API="http://debian12vm-003:5050" \
docker compose -f docker-compose-e2e.yml up -d

docker ps -a | grep -E "rococo|selenoid"
