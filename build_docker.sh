#!/bin/bash
VERSION=1.2.4
PRODUCT=opensearch
DOCKERFILE=dockerfiles/opensearch.microfocus.dockerfile
ARCH=x64

pushd docker/release
./build-image-single-arch.sh -v ${VERSION} -p ${PRODUCT} -f ${DOCKERFILE} -a ${ARCH}
popd