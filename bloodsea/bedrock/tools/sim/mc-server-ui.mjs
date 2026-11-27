// A stand-in for @minecraft/server-ui: every form answers at once, with the button the test asks for.
export const FormCancelationReason = { UserBusy: "UserBusy", UserClosed: "UserClosed" };
export const PICKS = { next: 0, shown: [] };
export class ActionFormData {
    constructor() { this.b = []; this._title = ""; }
    title(t) { this._title = JSON.stringify(t); return this; }
    body(b) { this._body = b; return this; }
    button(t, icon) { this.b.push(t); return this; }
    show(p) {
        PICKS.shown.push(this._title);
        const sel = Math.min(PICKS.next, Math.max(0, this.b.length - 1));
        return Promise.resolve({ canceled: false, selection: sel });
    }
}
