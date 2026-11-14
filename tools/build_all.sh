#!/bin/bash
# Builds both jars into dist/. Needs JDK 17 and JDK 21 installed (Gradle toolchains pick them up).
set -e
cd "$(dirname "$0")/.."
python3 tools/gen_entity.py
python3 tools/gen_hatman.py
python3 tools/gen_assets.py
python3 tools/gen_lang.py
mkdir -p dist
for v in 1.20.1 1.21.1; do
  (cd forge-$v && gradle build -q)
  cp forge-$v/build/libs/hatman_hollow_house-$v-forge-*.jar dist/
done
ls -la dist
