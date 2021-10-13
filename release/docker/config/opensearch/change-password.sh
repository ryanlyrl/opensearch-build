#!/bin/bash
set -x

# ensure JAVA_HOME is set
export JAVA_HOME=/usr/share/opensearch/jdk

# wait for OpenSearch to be up and running
while true; do
  STATUS=$(curl -X GET -k "https://admin:admin@opensearch-svc:9200/_cat/health" | tr -s ' ' | cut -d ' ' -f 4)
  echo "Received a cluster status of ${STATUS}"
  if [ "green" = ${STATUS} ] || [ "yellow" = ${STATUS} ]; then
    break;
  fi
  sleep 2
done

# Create a hash version of the configured password
pushd /usr/share/opensearch/plugins/opensearch-security/tools
  export HASH_PASSWORD=$(sh hash.sh -p ${OPENSEARCH_PASSWORD})
popd

# Set the value in the users.yml
pushd /usr/share/opensearch/plugins/opensearch-security/securityconfig
cat << EOF >> ./internal_users.yml

$OPENSEARCH_USER:
  hash: $HASH_PASSWORD
  reserved: false
  backend_roles:
  - "admin"
  description: "Interset's OpenSearch user."
EOF
popd

# Refresh the users in the cluster
pushd /usr/share/opensearch/plugins/opensearch-security/tools
  sh securityadmin.sh -cd ../securityconfig/ -icl -nhnv -cacert ../../../config/certs/issue_ca.crt -cert ../../../config/certs/opensearch-adm-crt.pem -key ../../../config/certs/opensearch-adm-key.pem
popd