#!/bin/bash

set -x

NAMESPACE=$(kubectl get namespaces | grep arcsight-installer | awk '{ print $1}')
CDF_SERVER=adam-opensearch2.ad.interset.com

function await_scale {
  DESIRED_REPLICAS=$1
  kubectl scale statefulsets opensearch-master --replicas="${DESIRED_REPLICAS}" -n "${NAMESPACE}"
  kubectl scale statefulsets opensearch-data --replicas="${DESIRED_REPLICAS}" -n "${NAMESPACE}"
  sleep 5 # poor mans way of waiting for the scale down to work
}

function push_latest {
  docker tag "opensearch:1.0.0" "${CDF_SERVER}:5000/srg/opensearch:1.0.0-test"
  docker push "${CDF_SERVER}:5000/srg/opensearch:1.0.0-test"
}

function pull_always {
    kubectl get statefulset opensearch-data -n $NAMESPACE -o json | sed 's/IfNotPresent/Always/g' | kubectl replace -f -
    kubectl get statefulset opensearch-master -n $NAMESPACE -o json | sed 's/IfNotPresent/Always/g' | kubectl replace -f -
}

function set_image {
  kubectl set image statefulset/opensearch-master "opensearch=localhost:5000/srg/opensearch:1.0.0-test" -n "${NAMESPACE}"
  kubectl set image statefulset/opensearch-data "opensearch=localhost:5000/srg/opensearch:1.0.0-test" -n "${NAMESPACE}"
}

push_latest
pull_always
await_scale 0
set_image
await_scale 1
