#!/usr/bin/env python3
"""Minecraft 1.21 renamed the data folders (recipes -> recipe, loot_tables -> loot_table) and recipe results now use
"id" instead of "item". This writes the 1.21.1 copies into forge-1.21.1/src/main/resources from the shared 1.20.1 data."""
import json
import os
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources', 'data', 'librabeyblade')
DST = os.path.join(HERE, '..', 'forge-1.21.1', 'src', 'main', 'resources', 'data', 'librabeyblade')


def main():
    if os.path.isdir(DST):
        shutil.rmtree(DST)
    n = 0
    for old, new in (('recipes', 'recipe'), ('loot_tables', 'loot_table')):
        for root, _, files in os.walk(os.path.join(SRC, old)):
            for f in files:
                src = os.path.join(root, f)
                rel = os.path.relpath(src, os.path.join(SRC, old))
                dst = os.path.join(DST, new, rel)
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(src) as fh:
                    data = json.load(fh)
                if old == 'recipes' and isinstance(data.get('result'), dict) and 'item' in data['result']:
                    r = data['result']
                    r['id'] = r.pop('item')
                with open(dst, 'w') as fh:
                    json.dump(data, fh, indent=1)
                n += 1
    print('%d data files for 1.21.1' % n)


if __name__ == '__main__':
    main()
