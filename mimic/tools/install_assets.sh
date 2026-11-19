#!/bin/bash
# regenerate the creature and copy it into the mod resources
set -e
cd "$(dirname "$0")"
python3 gen_mimic.py && python3 gen_mimic_anims.py
A=../common/src/main/resources/assets/mimicry
cp out/mimic.geo.json $A/geo/entity/
cp out/mimic.animation.json $A/animations/entity/
cp out/mimic.png out/mimic_glowmask.png $A/textures/entity/
