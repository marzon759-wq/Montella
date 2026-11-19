#!/bin/bash
# regenerates every asset in the right order (blocks wipe the item models, so the GUI/items come after)
set -e
cd "$(dirname "$0")"
python3 gen_mimic.py && python3 gen_mimic_anims.py
A=../common/src/main/resources/assets/mimicry
cp out/mimic.geo.json $A/geo/entity/ && cp out/mimic.animation.json $A/animations/entity/ && cp out/mimic.png out/mimic_glowmask.png $A/textures/entity/
python3 gen_farm.py
python3 gen_gui.py
python3 gen_lily.py
python3 gen_truck.py
python3 gen_lang.py
