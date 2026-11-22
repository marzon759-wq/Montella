#!/bin/bash
# regenerate the model + textures and copy them into the mod resources
set -e
cd "$(dirname "$0")"
python3 gen_kaiju.py
A=../forge-1.20.1/src/main/resources/assets/kaiju
cp out/kaiju.json $A/models/entity/kaiju.json
cp out/kaiju_*.png $A/textures/entity/
