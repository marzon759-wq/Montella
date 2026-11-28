#!/bin/bash
# Builds both jars into beyblade/dist/. Needs JDK 17 (1.20.1) and JDK 21 (1.21.1); Gradle toolchains pick them up.
set -e
cd "$(dirname "$0")/.."
python3 tools/gen_geo.py
python3 tools/gen_parts.py
python3 tools/gen_data121.py
mkdir -p dist
for v in 1.20.1 1.21.1; do
  (cd forge-$v && gradle build -q)
  cp forge-$v/build/libs/beyblade_mod-$v-forge-*.jar dist/
done
ls -la dist
