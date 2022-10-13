export function compareArrayBuffers(b1: ArrayBuffer, b2: ArrayBuffer): boolean {
    if (b1.byteLength !== b2.byteLength) {
        return false;
    }
    const view1 = new Uint8Array(b1);
    const view2 = new Uint8Array(b2);
    for (let i = 0; i < view1.length; i++) {
        if (view1[i] !== view2[i]) {
            return false;
        }
    }
    return true;
}