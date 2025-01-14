#!/bin/bash -e

usage() {
    echo "Usage: $0 <measure-report> <output-dir>"
    echo "       $0 <fhir-url> <output-dir> [-u <user> -p <password>]"
    echo "       $0 <fhir-url> <output-dir> [-i <issuer-url> -c <client-id> -s <client-secret>]"
    exit 1
}

if [[ $# -lt 2 ]]; then
    usage
fi

report=""
output_dir=""

url=""
user=""
password=""
issuer_url=""
client_id=""
client_secret=""


read_http_args() {
    url="$1"
    shift

    while getopts ":u:p:i:c:s:" opt; do
            case $opt in
                u)
                    user="$OPTARG"
                    ;;
                p)
                    password="$OPTARG"
                    ;;
                i)
                    issuer_url="$OPTARG"
                    ;;
                c)
                    client_id="$OPTARG"
                    ;;
                s)
                    client_secret="$OPTARG"
                    ;;
                *)
                    echo "Unknown option: -$OPTARG"
                    usage
                    ;;
            esac
        done


}

http_get_report() {
  auth="$1"
  url="$2"
  if [ "$auth" == "no-auth" ]; then
    response=$(curl -s "$url" \
                    -H "Content-Type: application/fhir+json")
    echo "$response"
  elif [ "$auth" == "basic-auth" ]; then
    fhir_user="$3"
    fhir_password="$4"
    response=$(curl -s "$url" \
                    -H "Content-Type: application/fhir+json" \
                    -u "$fhir_user:$fhir_password")
    echo "$response"
  else
    fhir_oauth_issuer_url="$3"
    fhir_oauth_client_id="$4"
    fhir_oauth_client_secret="$5"

    oauth_response=$(curl -s -X POST "$fhir_oauth_issuer_url" \
                          -H "Content-Type: application/x-www-form-urlencoded" \
                          -d "grant_type=client_credentials" \
                          -d "client_id=$fhir_oauth_client_id" \
                          -d "client_secret=$fhir_oauth_client_secret")
    fhir_report_bearer_token=$(echo "$oauth_response" | jq -r '.access_token')

    response=$(curl -s "$url" \
                   -H "Content-Type: application/fhir+json" \
                   -H "Authorization: Bearer $fhir_report_bearer_token")

    echo "$response"
  fi
}

if [[ ! -d $2 ]]; then
    echo "Second arg is not a directory."
    usage
else
  output_dir="$2"
fi

if [[ "$1" == http?(s)://* ]]; then
    if [[ $# == 2 ]]; then
      echo "Using FHIR URL without authentication."
      report="$(http_get_report "no-auth" "$1")"
    else
      read_http_args "$1" "${@:3}"

      if [[ -n "$user" && -n "$password" ]]; then
          echo "Using FHIR URL with user/password authentication."
          report="$(http_get_report "basic-auth" "$url" "$user" "$password")"
      elif [[ -n "$issuer_url" && -n "$client_id" && -n "$client_secret" ]]; then
          echo "Using FHIR URL with OAuth2 client credentials."
          http_get_report "oauth" "$url" "$issuer_url" "$client_id" "$client_secret"
          report="$(http_get_report "oauth" "$url" "$issuer_url" "$client_id" "$client_secret")"
      else
          echo "Error: Missing required arguments for FHIR URL authentication."
          usage
      fi
    fi
elif [[ -f $1 ]]; then
    echo "Reading MeasureReport from file."
    report=$(cat "$1")
else
    echo "First argument is neither a file or directory."
    usage
    exit 1
fi

resource_type=$(echo "$report" | jq -r '.resourceType')
if [[ "$resource_type" != "MeasureReport" ]]; then
  echo "Invalid input: File or url contains resource of type '$resource_type' but must be 'MeasureReport'"
  usage
fi

echo "$report" | jq -c '.group[]' | while IFS= read -r group; do
    echo "$group" | jq -c '.stratifier[]' | while IFS= read -r stratifier; do

      if ! echo "$stratifier" | jq -e 'has("stratum")' > /dev/null; then
        continue
      fi

      strat_codes=$(echo "$stratifier" | jq --raw-output 'if has("code") then (.code[0].coding[0].code) else (.stratum[0].component | map(.code.coding[0].code) | join("-")) end')
      filename="${output_dir}/${strat_codes}.csv"

      result=$(echo "$stratifier" | jq --raw-output '
                      (.) as $stratifier
                              | .stratum[0]
                              | if has("component") then
                                    ([$stratifier.stratum[0] | .component | sort_by(.code.coding[0].code)
                                    | map(["system", .code.coding[0].code, "display"])]| flatten + ["count"] as $header
                                    | $stratifier.stratum[0]
                                    | if has("measureScore") then $header + ["unique count"] else $header end),
                                    ($stratifier.stratum[] as $stratum | $stratum.component | sort_by(.code.coding[0].code)
                                    | map([.value.coding[0].system, .value.coding[0].code, .value.coding[0].display])
                                    | flatten as $row
                                    | $stratum.population[] | select(.code.coding[0].code == "initial-population") | .count as $count
                                    | $row + [$count] as $row
                                    | $stratum
                                    | if has("measureScore") then $row + [.measureScore.value] else $row end)
                                else
                                    (["system", "code", "display", "count"] as $header
                                    | if has("measureScore") then $header + ["unique count"] else $header end),
                                    ( $stratifier.stratum[] as $stratum
                                    | $stratum.population[] | select(.code.coding[0].code == "initial-population") | .count as $count
                                    | [$stratum.value.coding[0].system, $stratum.value.coding[0].code, $stratum.value.coding[0].display, $count] as $row
                                    | $stratum | if has("measureScore") then $row + [.measureScore.value] else $row end)
                                end
                             | @csv')

      echo "${result}" > "$filename"
    done
done
