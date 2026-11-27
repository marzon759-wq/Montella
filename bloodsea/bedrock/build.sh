#!/bin/bash
# builds the Bedrock add-on: data from the Java mod, type-check, bundle the script, the packs, the .mcaddon
set -e
cd "$(dirname "$0")"
python3 tools/gen_ts_data.py
npx tsc -p . --noEmit
python3 tools/build_packs.py
mkdir -p out/BP/scripts
npx esbuild src/main.ts --bundle --format=esm --target=es2020 --external:@minecraft/server --external:@minecraft/server-ui --outfile=out/BP/scripts/main.js --log-level=warning
cd out
rm -f ../BloodSea_Bedrock.mcaddon
mkdir -p pack && rm -rf pack/*
cp -r BP "pack/Blood Sea BP" && cp -r RP "pack/Blood Sea RP"
(cd pack && zip -qr ../../BloodSea_Bedrock.mcaddon "Blood Sea BP" "Blood Sea RP")
echo "built $(du -h ../BloodSea_Bedrock.mcaddon | cut -f1) BloodSea_Bedrock.mcaddon"
