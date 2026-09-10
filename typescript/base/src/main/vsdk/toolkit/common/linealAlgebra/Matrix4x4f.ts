import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";
import { Matrix4x4d } from "./Matrix4x4d.js";
import { Vector3Df } from "./Vector3Df.js";
import { Vector4Df } from "./Vector4Df.js";
import { Vector3Dd } from "./Vector3Dd.js";
import { Vector4Dd } from "./Vector4Dd.js";
const f = Math.fround;
export class Matrix4x4f extends FundamentalEntity {
    private readonly d: Matrix4x4d;
    public constructor();
    public constructor(o: Matrix4x4f | number[][]);
    public constructor(
        o: Matrix4x4f | number[][] = [
            [1, 0, 0, 0],
            [0, 1, 0, 0],
            [0, 0, 1, 0],
            [0, 0, 0, 1],
        ],
    ) {
        super();
        this.d = o instanceof Matrix4x4f ? new Matrix4x4d(o.d) : new Matrix4x4d(o.map((r) => r.map(f)));
    }
    private static fromD(d: Matrix4x4d): Matrix4x4f {
        return new Matrix4x4f(d.toArrayCopy());
    }
    public static copyOf(o: Matrix4x4f | number[][]): Matrix4x4f {
        return new Matrix4x4f(o);
    }
    public static identityMatrix(): Matrix4x4f {
        return new Matrix4x4f();
    }
    public identity(): Matrix4x4f {
        return new Matrix4x4f();
    }
    public get(r: number, c: number): number {
        return f(this.d.get(r, c));
    }
    public withVal(r: number, c: number, v: number): Matrix4x4f {
        return Matrix4x4f.fromD(this.d.withVal(r, c, f(v)));
    }
    public toArrayCopy(): number[][] {
        return this.d.toArrayCopy().map((r) => r.map(f));
    }
    public withoutTranslation(): Matrix4x4f {
        return Matrix4x4f.fromD(this.d.withoutTranslation());
    }
    public extractTranslation(): Vector3Df {
        const v = this.d.extractTranslation();
        return new Vector3Df(v);
    }
    public withTranslation(v: Vector3Df): Matrix4x4f {
        return Matrix4x4f.fromD(this.d.withTranslation(new Vector3Dd(v.x(), v.y(), v.z())));
    }
    public translation(x: number | Vector3Df, y?: number, z?: number): Matrix4x4f {
        if (x instanceof Vector3Df) return this.translation(x.x(), x.y(), x.z());
        return Matrix4x4f.fromD(this.d.translation(f(x), f(y!), f(z!)));
    }
    public scale(x: number | Vector3Df, y?: number, z?: number): Matrix4x4f {
        if (x instanceof Vector3Df) return this.scale(x.x(), x.y(), x.z());
        return Matrix4x4f.fromD(this.d.scale(f(x), f(y!), f(z!)));
    }
    public axisRotation(a: number, v: Vector3Df): Matrix4x4f;
    public axisRotation(a: number, x: number, y: number, z: number): Matrix4x4f;
    public axisRotation(a: number, x: number | Vector3Df, y?: number, z?: number): Matrix4x4f {
        return x instanceof Vector3Df
            ? Matrix4x4f.fromD(this.d.axisRotation(f(a), new Vector3Dd(x.x(), x.y(), x.z())))
            : Matrix4x4f.fromD(this.d.axisRotation(f(a), f(x), f(y!), f(z!)));
    }
    public multiply(o: number): Matrix4x4f;
    public multiply(o: Matrix4x4f): Matrix4x4f;
    public multiply(o: Vector3Df): Vector3Df;
    public multiply(o: Vector4Df): Vector4Df;
    public multiply(o: number | Matrix4x4f | Vector3Df | Vector4Df): Matrix4x4f | Vector3Df | Vector4Df {
        if (typeof o === "number") return Matrix4x4f.fromD(this.d.multiply(f(o)));
        if (o instanceof Matrix4x4f) return Matrix4x4f.fromD(this.d.multiply(o.d));
        if (o instanceof Vector3Df) {
            const v = this.d.multiply(new Vector3Dd(o.x(), o.y(), o.z()));
            return new Vector3Df(v);
        }
        const v = this.d.multiply(new Vector4Dd(o.x(), o.y(), o.z(), o.w()));
        return new Vector4Df(v);
    }
    public transpose(): Matrix4x4f {
        return Matrix4x4f.fromD(this.d.transpose());
    }
    public determinant(): number {
        return f(this.d.determinant());
    }
    public invert(): Matrix4x4f {
        return Matrix4x4f.fromD(this.d.invert());
    }
    public inverse(): Matrix4x4f {
        return this.invert();
    }
    public epsilonEquals(o: Matrix4x4f | null, e = f(VSDK.EPSILON)): boolean {
        return o !== null && this.d.epsilonEquals(o.d, e);
    }
}
