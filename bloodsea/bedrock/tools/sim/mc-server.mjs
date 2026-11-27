// A stand-in for @minecraft/server, enough to run the add-on's script in Node: a world with one player, entities that
// remember their position, rotation and properties, blocks that are air until filled, events, a tick loop.
export const LOG = [];
const log = (...a) => LOG.push(a.join(" "));

let nextId = 1;
const blocks = new Map();       // "x,y,z" -> typeId
const loadedRadius = 200;        // blocks around the player that count as loaded

class Component { constructor(o) { Object.assign(this, o); } }

export class Entity {
    constructor(typeId, dim, loc) {
        this.typeId = typeId; this.dimension = dim; this.location = { ...loc }; this.id = String(nextId++);
        this.rot = { x: 0, y: 0 }; this.props = {}; this.dp = {}; this.tags = new Set(); this.valid = true; this.vel = { x: 0, y: 0, z: 0 };
        this.effects = {}; this.riders = []; this.ridingOn = undefined;
    }
    isValid() { return this.valid; }
    remove() { this.valid = false; this.dimension.entities.delete(this.id); }
    kill() { this.killed = (this.killed || 0) + 1; log("KILL", this.typeId); return true; }
    teleport(loc, o) { if (!this.valid) throw new Error("invalid"); this.location = { ...loc }; if (o?.rotation) this.rot = { ...o.rotation }; }
    setRotation(r) { this.rot = { ...r }; }
    getRotation() { return { ...this.rot }; }
    getVelocity() { return { ...this.vel }; }
    setProperty(k, v) {
        if (!this.typeId.startsWith("bloodsea:")) throw new Error("no property " + k);
        if (typeof v !== "number" && typeof v !== "boolean" && typeof v !== "string") throw new Error("bad property value " + v);
        this.props[k] = v;
    }
    getProperty(k) { return this.props[k] ?? 0; }
    getDynamicProperty(k) { return this.dp[k]; }
    setDynamicProperty(k, v) { if (v === undefined) delete this.dp[k]; else this.dp[k] = v; }
    addTag(t) { this.tags.add(t); return true; }
    hasTag(t) { return this.tags.has(t); }
    addEffect(n, d, o) { this.effects[n] = d; }
    removeEffect(n) { delete this.effects[n]; return true; }
    getComponent(n) {
        n = n.replace("minecraft:", "");
        if (n === "riding") return this.ridingOn ? new Component({ entityRidingOn: this.ridingOn }) : undefined;
        if (n === "rideable") {
            const self = this;
            return new Component({
                addRider(p) { if (p.ridingOn) return false; p.ridingOn = self; self.riders.push(p); return true; },
                ejectRiders() { for (const p of self.riders) p.ridingOn = undefined; self.riders = []; },
                ejectRider(p) { p.ridingOn = undefined; self.riders = self.riders.filter(x => x !== p); },
            });
        }
        if (n === "inventory") return this.inv;
        if (n === "player.hunger") return new Component({ setCurrentValue(v) { } });
        return undefined;
    }
    applyDamage(n) { this.hurt = (this.hurt || 0) + n; return true; }
    applyKnockback() { }
    getHeadLocation() { return { x: this.location.x, y: this.location.y + 1.6, z: this.location.z }; }
    getViewDirection() { const a = this.rot.y * Math.PI / 180; return { x: -Math.sin(a), y: 0, z: Math.cos(a) }; }
    runCommand(c) { this.dimension.runCommand(c); return { successCount: 1 }; }
}

class Container {
    constructor() { this.items = new Array(36).fill(undefined); this.size = 36; }
    addItem(it) { const i = this.items.findIndex(x => !x); if (i >= 0) this.items[i] = it; return undefined; }
    getItem(i) { return this.items[i]; }
    setItem(i, it) { this.items[i] = it; }
}

export class Player extends Entity {
    constructor(dim) {
        super("minecraft:player", dim, { x: 3, y: 70, z: 5 });
        this.name = "Tester"; this.isSneaking = false; this.isJumping = false; this.selectedSlotIndex = 0;
        this.inv = new Component({ container: new Container() });
        this.screen = []; this.cameraCalls = []; this.sounds = [];
        const self = this;
        this.camera = {
            setCamera(preset, o) { if (o?.location) for (const k of ["x", "y", "z"]) if (!isFinite(o.location[k])) throw new Error("camera NaN"); self.cameraCalls.push(preset); },
            fade() { }, clear() { self.cameraCalls.push("clear"); },
        };
        this.onScreenDisplay = {
            setActionBar(m) { self.screen.push(["bar", JSON.stringify(m)]); },
            setTitle(t, o) { self.screen.push(["title", JSON.stringify(o?.subtitle ?? t)]); },
        };
    }
    getGameMode() { return "survival"; }
    playSound(id, o) { self_sounds.add(id); }
}
const self_sounds = new Set();
export const SOUNDS = self_sounds;

export class Dimension {
    constructor(id) { this.id = id; this.entities = new Map(); this.commands = []; }
    near(loc) { const p = world._player; return !p || Math.hypot(loc.x - p.location.x, loc.z - p.location.z) < loadedRadius; }
    spawnEntity(t, loc) {
        if (!this.near(loc)) throw new Error("unloaded " + JSON.stringify(loc));
        for (const k of ["x", "y", "z"]) if (!isFinite(loc[k])) throw new Error("spawn NaN");
        const e = new Entity(t, this, loc); this.entities.set(e.id, e); return e;
    }
    spawnItem(it, loc) { const e = new Entity("minecraft:item", this, loc); e.item = it; this.entities.set(e.id, e); return e; }
    spawnParticle(id, loc, vars) { if (!isFinite(loc.x)) throw new Error("particle NaN"); }
    playSound(id, loc, o) { if (!isFinite(loc.x)) throw new Error("sound NaN"); self_sounds.add(id); }
    getEntities(q = {}) {
        let l = [...this.entities.values()].filter(e => e.valid);
        if (q.type) l = l.filter(e => e.typeId === q.type);
        if (q.families) l = l.filter(e => e.typeId.startsWith("bloodsea:"));
        if (q.tags) l = l.filter(e => q.tags.every(t => e.tags.has(t)));
        if (q.excludeTags) l = l.filter(e => !q.excludeTags.some(t => e.tags.has(t)));
        if (q.location && q.maxDistance !== undefined) l = l.filter(e => Math.hypot(e.location.x - q.location.x, e.location.y - q.location.y, e.location.z - q.location.z) <= q.maxDistance);
        if (q.location) l.sort((a, b) => Math.hypot(a.location.x - q.location.x, a.location.z - q.location.z) - Math.hypot(b.location.x - q.location.x, b.location.z - q.location.z));
        if (q.closest) l = l.slice(0, q.closest);
        return l;
    }
    getPlayers(q = {}) { return world._player ? [world._player].filter(p => !q.location || Math.hypot(p.location.x - q.location.x, p.location.z - q.location.z) <= (q.maxDistance ?? 1e9)) : []; }
    getBlock(loc) {
        if (!this.near(loc)) return undefined;
        return { typeId: blocks.get(`${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`) ?? "minecraft:air" };
    }
    runCommand(c) {
        this.commands.push(c);
        const m = c.match(/^fill (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) (\S+)/);
        if (m) {
            const [x0, y0, z0, x1, y1, z1] = m.slice(1, 7).map(Number);
            if (Math.abs(x1 - x0 + 1) * Math.abs(y1 - y0 + 1) * Math.abs(z1 - z0 + 1) > 32768) throw new Error("fill too big: " + c);
            for (let x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (let y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (let z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                blocks.set(`${x},${y},${z}`, m[7].includes(":") ? m[7] : "minecraft:" + m[7]);
        }
        const s = c.match(/^setblock (-?\d+) (-?\d+) (-?\d+) (\S+)/);
        if (s) blocks.set(`${s[1]},${s[2]},${s[3]}`, s[4]);
        return { successCount: 1 };
    }
}

class Signal { constructor() { this.h = []; } subscribe(f) { this.h.push(f); return f; } fire(e) { for (const f of this.h) f(e); } }

const overworld = new Dimension("minecraft:overworld");
export const world = {
    _player: undefined,
    _time: 0, _abs: 0,
    getDimension() { return overworld; },
    getAllPlayers() { return this._player ? [this._player] : []; },
    getEntity(id) { return overworld.entities.get(id); },
    getTimeOfDay() { return this._time; },
    setTimeOfDay(t) { if (!Number.isInteger(t) || t < 0) throw new Error("bad time " + t); this._time = t; },
    getAbsoluteTime() { return this._abs; },
    playMusic(id, o) { log("MUSIC", id); },
    stopMusic() { log("MUSIC stop"); },
    afterEvents: { playerSpawn: new Signal(), itemUse: new Signal(), dataDrivenEntityTrigger: new Signal() },
};

const intervals = [];
export const system = {
    runInterval(f, n) { intervals.push(f); return intervals.length; },
    runTimeout(f, n) { later.push([world._abs + (n || 1), f]); return 1; },
    afterEvents: { scriptEventReceive: new Signal() },
};
const later = [];

export function tick() {
    world._abs++;
    for (let i = later.length - 1; i >= 0; i--) if (later[i][0] <= world._abs) { const f = later[i][1]; later.splice(i, 1); f(); }
    for (const f of intervals) f();
}

export function addPlayer() { const p = new Player(overworld); world._player = p; overworld.entities.set(p.id, p); return p; }

export const EasingType = { InOutSine: "InOutSine", Linear: "Linear" };
export const GameMode = { creative: "creative", spectator: "spectator", survival: "survival" };
export class ItemStack { constructor(t, n = 1) { this.typeId = t; this.amount = n; } }
export class MolangVariableMap { setFloat() { } }
export const EntityInventoryComponent = Component;
