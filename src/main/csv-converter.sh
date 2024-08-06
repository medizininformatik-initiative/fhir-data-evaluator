#!/bin/bash -e

if [ $# -ne 1 ]; then
    echo "Usage: $0 <output-dir>"
    exit 1
fi

outputDir="$1"
report="${outputDir}/measure-report.json"

jq -c '.group[]' "$report" | while IFS= read -r group; do
    echo "$group" | jq -c '.stratifier[]' | while IFS= read -r stratifier; do

      if ! echo "$stratifier" | jq -e 'has("stratum")' > /dev/null; then
        continue
      fi

      strat_codes=$(echo "$stratifier" | jq --raw-output 'if has("code") then (.code[0].coding[0].code) else (.stratum[0].component | map(.code.coding[0].code) | join("-")) end')
      filename="${outputDir}/${strat_codes}.csv"

      result=$(echo "$stratifier" | jq --raw-output '
                      (.) as $stratifier | .stratum[0] | if has("component") then
                                    ([$stratifier.stratum[0] | .component | sort_by(.code.coding[0].code)
                                    | map(["system", .code.coding[0].code, "display"])]| flatten as $header | $header + ["count"]),
                                    ($stratifier.stratum[] as $stratum | $stratum.component | sort_by(.code.coding[0].code)
                                    | map([.value.coding[0].system, .value.coding[0].code, .value.coding[0].display])
                                    | flatten + [$stratum.population[0].count] )
                                else
                                    ["system", "code", "display", "count"],
                                    ($stratifier.stratum[] | [.value.coding[0].system, .value.coding[0].code, .value.coding[0].display, .population[0].count])
                                end
                             | @csv')

      echo "${result}" > "$filename"
    done
done
