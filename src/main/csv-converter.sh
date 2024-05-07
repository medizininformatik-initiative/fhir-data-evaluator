#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <output-dir>"
    exit 1
fi

report=$(cat)
today=$(date +"%Y-%m-%d")
output_dir="$1${today}"

mkdir -p $output_dir

group_counter=0
echo $report | jq -c '.group[]' | while IFS= read -r group; do #.stratifier[0].stratum[]
    echo $group | jq -c '.stratifier[]' | while IFS= read -r stratifier; do
      strat_code=$(echo $stratifier | jq --raw-output '.code[0].coding[0].code')
      filename="${output_dir}/group${group_counter}-${strat_code}.csv"

      result=$(echo $stratifier | jq --raw-output '
          ["system", "code", "display", "count"],
          (.stratum[] | [.value.coding[0].system, .value.coding[0].code, .value.coding[0].display, .population[0].count])
          | @csv')

      echo "${result}" > "$filename"
    done
    ((group_counter++))
done
