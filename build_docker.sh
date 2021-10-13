VERSION=1.0.0
PRODUCT=opensearch
DOCKERFILE=dockerfiles/opensearch.microfocus.dockerfile

pushd release/docker
sh ./build-image.sh -v ${VERSION} -p ${PRODUCT} -f ${DOCKERFILE}
popd