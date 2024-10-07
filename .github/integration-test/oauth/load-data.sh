#!/bin/bash -e

DIR="$1"

TOKEN="$(docker compose -f "$DIR/../oauth/docker-compose.yml" exec -it proxy curl -s --cacert /etc/nginx/certs/cert.pem -d 'client_id=account' -d 'client_secret=test' -d 'grant_type=client_credentials' 'https://secure-keycloak:8443/realms/test/protocol/openid-connect/token' | jq -r '.access_token')"

blazectl --no-progress --token "$TOKEN" --server http://localhost:8082/fhir upload "$DIR"
