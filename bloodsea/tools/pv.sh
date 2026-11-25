#!/bin/bash
# pv.sh name out yaw pitch dist ty [anim t]
D=/tmp/claude-0/-home-user-Montella/ca4834bb-a456-5944-b90d-b1794b013286/scratchpad/cprev
G=../common/src/main/resources/assets/bloodsea
EXTRA=""
[ -n "$7" ] && EXTRA="--anim $G/animations/$1.animation.json animation.$1.$7 --t ${8:-0}"
FOG0=${FOG0:-2000} BRIGHT=${BRIGHT:-1.6} python3 preview.py $G/geo/$1.geo.json $G/textures/entity/$1.png $D/$2.png --yaw $3 --pitch $4 --dist $5 --ty $6 --size ${SIZE:-640} $EXTRA
