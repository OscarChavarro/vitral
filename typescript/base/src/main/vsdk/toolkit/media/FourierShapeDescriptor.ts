import { ShapeDescriptor } from "./ShapeDescriptor.js";
import { VSDK } from "../common/VSDK.js";
import { Logger } from "../common/logging/Logger.js";
export class FourierShapeDescriptor extends ShapeDescriptor {
    private featureVector = new Float64Array(512);
    public setFeature(sphere: number, harmonic: number, r: number, i: number): void {
        if (sphere < 0 || sphere >= 32 || harmonic < 0 || harmonic >= 16) return;
        this.featureVector[sphere * 16 + harmonic] = Math.hypot(r, i);
    }
    public getFeatureVector(): Float64Array {
        return this.featureVector;
    }
    public setFeatureVector(v: Float64Array | number[]): void {
        if (v.length !== 512) {
            Logger.reportMessage(
                this,
                VSDK.ERROR,
                "setFeatureVector",
                "Trying to set featurevector from incorrectly sized data!",
            );
            return;
        }
        this.featureVector = Float64Array.from(v);
    }
    public override toString(): string {
        return `SphericalHarmonics amplitudes for 32 spheres and 16 harmonics:\n${Array.from(this.featureVector, (x) => `  - ${VSDK.formatDouble(x)}\n`).join("")}`;
    }
}
