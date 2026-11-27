// The places the endings need that are not on the sea, built far off in the sky the first time they are needed:
// a hospital room, the inside of a stomach, and a patch of grey ocean with a fishing boat on it.
import { Dimension, Vector3 } from "@minecraft/server";
import { cmd } from "./util";

export const HOSPITAL = { x: 20000, y: 200, z: 20000 };
export const STOMACH = { x: 20060, y: 200, z: 20000 };
export const RESCUE = { x: 21000, y: 200, z: 21000 };

function box(d: Dimension, o: Vector3, x0: number, y0: number, z0: number, x1: number, y1: number, z1: number, b: string): void {
    cmd(d, `fill ${o.x + x0} ${o.y + y0} ${o.z + z0} ${o.x + x1} ${o.y + y1} ${o.z + z1} ${b}`);
}
function set(d: Dimension, o: Vector3, x: number, y: number, z: number, b: string): void {
    cmd(d, `setblock ${o.x + x} ${o.y + y} ${o.z + z} ${b}`);
}

/** a small white room: a bed, a monitor, a drip stand, a window full of light, a chair */
export function hospital(d: Dimension): Vector3 {
    const o = HOSPITAL;
    box(d, o, -6, -2, -6, 6, 6, 6, "stone");
    box(d, o, -5, -1, -5, 5, 5, 5, "white_concrete");
    box(d, o, -4, 0, -4, 4, 4, 4, "air");
    box(d, o, -4, -1, -4, 4, -1, 4, "smooth_stone");
    for (const [x, z] of [[-2, -2], [2, -2], [-2, 2], [2, 2], [0, 0]]) set(d, o, x, 5, z, "sea_lantern");
    for (let z = -1; z <= 1; z++) set(d, o, 0, 0, z, "smooth_quartz_slab");
    set(d, o, 0, 0, 2, "iron_bars"); set(d, o, 0, 1, 2, "iron_bars");
    set(d, o, 1, 0, 2, "iron_bars"); set(d, o, 1, 1, 2, "black_concrete");
    set(d, o, -1, 0, 1, "iron_bars"); set(d, o, -1, 1, 1, "iron_bars"); set(d, o, -1, 2, 1, "end_rod");
    box(d, o, -2, 1, 5, 2, 3, 5, "white_stained_glass");
    box(d, o, -2, 1, 6, 2, 3, 6, "sea_lantern");
    set(d, o, 3, 0, -5, "dark_oak_door");
    set(d, o, 2, 0, -1, "oak_stairs");
    set(d, o, -3, 0, 3, "oak_slab");
    set(d, o, -3, 1, 3, "flower_pot");
    return { x: o.x + 0.5, y: o.y, z: o.z + 0.5 };
}

/** a chamber of flesh, wet and close, with the bones of what it ate before */
export function stomach(d: Dimension): Vector3 {
    const o = STOMACH;
    box(d, o, -10, -3, -11, 10, 10, 11, "bloodsea:flesh");
    // hollow it as an egg, layer by layer
    for (let y = 0; y <= 8; y++) {
        const k = 1 - ((y - 3) * (y - 3)) / 30;
        if (k <= 0) continue;
        const rx = Math.floor(7 * Math.sqrt(k)), rz = Math.floor(8.5 * Math.sqrt(k));
        box(d, o, -rx, y, -rz, rx, y, rz, "air");
    }
    for (const [x, z] of [[-4, 3], [3, -5], [5, 4], [-3, -6], [2, 6], [-5, -1]]) set(d, o, x, 0, z, "bone_block");
    set(d, o, 4, 0, -2, "skeleton_skull"); set(d, o, -2, 0, 5, "skeleton_skull");
    box(d, o, -6, 7, -2, -6, 7, 2, "shroomlight");
    box(d, o, 6, 6, -3, 6, 6, 3, "nether_wart_block");
    return { x: o.x + 0.5, y: o.y, z: o.z + 0.5 };
}

/** grey water in the rain, and a fishing boat */
export function rescue(d: Dimension): Vector3 {
    const o = RESCUE;
    box(d, o, -22, -5, -22, 22, -5, 22, "barrier");
    box(d, o, -22, -4, -22, 22, -1, 22, "water");
    box(d, o, -22, -4, -22, 22, 1, -22, "barrier"); box(d, o, -22, -4, 22, 22, 1, 22, "barrier");
    box(d, o, -22, -4, -22, -22, 1, 22, "barrier"); box(d, o, 22, -4, -22, 22, 1, 22, "barrier");
    box(d, o, -1, -2, -4, 1, 0, 4, "spruce_planks");
    box(d, o, -2, 0, -4, -2, 0, 4, "dark_oak_planks"); box(d, o, 2, 0, -4, 2, 0, 4, "dark_oak_planks");
    box(d, o, -1, 1, -4, 1, 2, -3, "white_terracotta");
    set(d, o, 0, 2, -3, "glass_pane");
    box(d, o, -1, 3, -4, 1, 3, -3, "spruce_slab");
    set(d, o, -1, 1, 2, "spruce_trapdoor");
    set(d, o, 0, 1, 4, "chain"); set(d, o, 0, 2, 4, "lantern");
    cmd(d, "weather rain 2000");
    return { x: o.x + 0.5, y: o.y, z: o.z + 0.5 };
}
