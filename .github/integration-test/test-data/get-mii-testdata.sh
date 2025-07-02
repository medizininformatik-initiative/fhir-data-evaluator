#!/usr/bin/env bash

MII_TESTDATA_DOWNLOAD_URL="https://github.com/medizininformatik-initiative/mii-testdata/releases/download/v1.0.1/kds-testdata-2024.0.1.zip"

wget -O testdata.zip "$MII_TESTDATA_DOWNLOAD_URL"
unzip testdata.zip -d testdata-temp
cd testdata-temp/kds-testdata-2024.0.1 || exit
mkdir ../../.github/integration-test/mii-testdata
cp resources/Bundle-mii-exa-test-data-bundle.json ../../.github/integration-test/mii-testdata


cd ../../
rm testdata.zip
rm -rf testdata-temp