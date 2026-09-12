import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { SolidTextureStatistics } from "../../../common/statistics/SolidTextureStatistics.js";
import { LookUpTableChecksum16 } from "../../../numericalAnalysis/lookUpTables/LookUpTableChecksum16.js";
import { LookUpTableSine } from "../../../numericalAnalysis/lookUpTables/LookUpTableSine.js";

/**
Java's `Lattice`, a private nested holder for the eight-corner setup both
`noise` and `differentialNoise` share.
*/
class _Lattice {
    public x = 0;
    public y = 0;
    public z = 0;
    public ix = 0;
    public iy = 0;
    public iz = 0;
    public jx = 0;
    public jy = 0;
    public jz = 0;
    public sx = 0;
    public sy = 0;
    public sz = 0;
    public tx = 0;
    public ty = 0;
    public tz = 0;
}

/**
Java's private nested `CRandom`: the classic C library linear congruential
generator, which is what makes the permutation table reproducible across
languages.

Java evaluates `state * 1103515245L + 12345L` in 64-bit arithmetic, and with a
state below 2^31 that product reaches about 2^61 — far past the 2^53 a
TypeScript `number` represents exactly. The multiplication is therefore split
at 2^16: the high half only contributes through its low 15 bits, because
everything above bit 30 is discarded by the mask anyway, so both partial
products stay well inside the exact range and the sequence is Java's, term for
term.
*/
class _CRandom {
    private static readonly MULTIPLIER = 1103515245;
    private static readonly INCREMENT = 12345;
    private static readonly MODULUS = 0x80000000;

    private state: number;

    public constructor(seed: number) {
        this.state = seed & 0x7fffffff;
    }

    public next(): number {
        const high: number = Math.floor(this.state / 0x10000);
        const low: number = this.state % 0x10000;
        const product: number = ((high * _CRandom.MULTIPLIER) % 0x8000) * 0x10000 + low * _CRandom.MULTIPLIER;
        this.state = (product + _CRandom.INCREMENT) % _CRandom.MODULUS;
        return Math.floor(this.state / 0x10000) & 0x7fff;
    }
}

/**
Port of `vsdk.toolkit.media.solidTexture.procedural.ProceduralNoise`.

Perlin-style lattice noise with its permutation table, its differential
(gradient) flavor, and the turbulence sums built on both. Two things about it
are worth stating, because a texture's reproducibility rests on them: the
permutation table is shuffled by the C library generator ported above, and the
`rTable` entries come from a CRC-16 over the little-endian bytes of three
doubles, which is why `checksumVector` builds a `DataView` rather than doing
arithmetic.

Java holds the lattice cell indexes in `long`s; they are plain numbers here.
The values stay near the `MIN_X` bias of 10000 plus the caller's coordinate, so
they never approach the exact-integer limit.

Java's `differentialNoise(Vector3Dd[], ...)` and
`differentialTurbulence(Vector3Dd[], ...)` overloads have no counterpart: they
exist only to return a vector through a one-element array, which is how Java
spells an out parameter, and both already have a returning flavor that this
port keeps.
*/
export class ProceduralNoise {
    private static readonly MIN_X = -10000;
    private static readonly MIN_Y = ProceduralNoise.MIN_X;
    private static readonly MIN_Z = ProceduralNoise.MIN_X;
    private static readonly MAXSIZE = 267;
    private static readonly REAL_SCALE = 2.0 / 65535.0;

    private permutationTable: Int16Array | null = null;
    private rTable: Float64Array | null = null;
    private readonly sineLookUpTable: LookUpTableSine;
    private readonly checksumLookUpTable: LookUpTableChecksum16;
    private readonly solidTextureStatistics: SolidTextureStatistics | null;

    public constructor(solidTextureStatistics: SolidTextureStatistics | null = null) {
        this.sineLookUpTable = new LookUpTableSine(11);
        this.checksumLookUpTable = new LookUpTableChecksum16();
        this.solidTextureStatistics = solidTextureStatistics;
    }

    public initialize(): void {
        this.initRTable();
    }

    public sCurve(a: number): number {
        return a * a * (3.0 - 2.0 * a);
    }

    public cycloidal(value: number): number {
        if (value >= 0.0) {
            return this.sineLookUpTable.eval(value - Math.floor(value));
        }
        return 0.0 - this.sineLookUpTable.eval(0.0 - (value + Math.floor(0.0 - value)));
    }

    public triangleWave(value: number): number {
        let offset: number;
        if (value >= 0.0) {
            offset = value - Math.floor(value);
        } else {
            const temp1: number = -1.0 - Math.floor(Math.abs(value));
            offset = value - temp1;
        }
        if (offset >= 0.5) {
            return 2.0 * (1.0 - offset);
        }
        return 2.0 * offset;
    }

    public noise(x: number, y: number, z: number): number {
        this.ensureInitialized();
        if (this.solidTextureStatistics !== null) {
            this.solidTextureStatistics.callsToNoise++;
        }

        const l: _Lattice = this.setupLattice(x, y, z);
        let sum: number;
        let m: number;

        m = this.hash3d(l.ix, l.iy, l.iz) & 0xff;
        sum = this.incrSum(m, l.tx * l.ty * l.tz, l.x - l.ix, l.y - l.iy, l.z - l.iz);
        m = this.hash3d(l.jx, l.iy, l.iz) & 0xff;
        sum += this.incrSum(m, l.sx * l.ty * l.tz, l.x - l.jx, l.y - l.iy, l.z - l.iz);
        m = this.hash3d(l.ix, l.jy, l.iz) & 0xff;
        sum += this.incrSum(m, l.tx * l.sy * l.tz, l.x - l.ix, l.y - l.jy, l.z - l.iz);
        m = this.hash3d(l.jx, l.jy, l.iz) & 0xff;
        sum += this.incrSum(m, l.sx * l.sy * l.tz, l.x - l.jx, l.y - l.jy, l.z - l.iz);
        m = this.hash3d(l.ix, l.iy, l.jz) & 0xff;
        sum += this.incrSum(m, l.tx * l.ty * l.sz, l.x - l.ix, l.y - l.iy, l.z - l.jz);
        m = this.hash3d(l.jx, l.iy, l.jz) & 0xff;
        sum += this.incrSum(m, l.sx * l.ty * l.sz, l.x - l.jx, l.y - l.iy, l.z - l.jz);
        m = this.hash3d(l.ix, l.jy, l.jz) & 0xff;
        sum += this.incrSum(m, l.tx * l.sy * l.sz, l.x - l.ix, l.y - l.jy, l.z - l.jz);
        m = this.hash3d(l.jx, l.jy, l.jz) & 0xff;
        sum += this.incrSum(m, l.sx * l.sy * l.sz, l.x - l.jx, l.y - l.jy, l.z - l.jz);

        sum += 0.5;
        if (sum < 0.0) sum = 0.0;
        if (sum > 1.0) sum = 1.0;
        return sum;
    }

    public differentialNoise(x: number, y: number, z: number): Vector3Dd {
        this.ensureInitialized();
        if (this.solidTextureStatistics !== null) {
            this.solidTextureStatistics.callsToDNoise++;
        }

        const l: _Lattice = this.setupLattice(x, y, z);
        let px: number = l.x - l.ix;
        let py: number = l.y - l.iy;
        let pz: number = l.z - l.iz;
        let s: number = l.tx * l.ty * l.tz;
        let m: number = this.hash3d(l.ix, l.iy, l.iz) & 0xff;
        let rx: number = this.incrSum(m, s, px, py, pz);
        let ry: number = this.incrSum(m + 4, s, px, py, pz);
        let rz: number = this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.jx, l.iy, l.iz) & 0xff;
        px = l.x - l.jx;
        s = l.sx * l.ty * l.tz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.jx, l.jy, l.iz) & 0xff;
        py = l.y - l.jy;
        s = l.sx * l.sy * l.tz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.ix, l.jy, l.iz) & 0xff;
        px = l.x - l.ix;
        s = l.tx * l.sy * l.tz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.ix, l.jy, l.jz) & 0xff;
        pz = l.z - l.jz;
        s = l.tx * l.sy * l.sz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.jx, l.jy, l.jz) & 0xff;
        px = l.x - l.jx;
        s = l.sx * l.sy * l.sz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.jx, l.iy, l.jz) & 0xff;
        py = l.y - l.iy;
        s = l.sx * l.ty * l.sz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        m = this.hash3d(l.ix, l.iy, l.jz) & 0xff;
        px = l.x - l.ix;
        s = l.tx * l.ty * l.sz;
        rx += this.incrSum(m, s, px, py, pz);
        ry += this.incrSum(m + 4, s, px, py, pz);
        rz += this.incrSum(m + 8, s, px, py, pz);

        return new Vector3Dd(rx, ry, rz);
    }

    public turbulence(x: number, y: number, z: number, octaves: number): number {
        let t = 0.0;
        for (let i = 0; i < octaves; i++) {
            const scale: number = Math.pow(0.5, i);
            t += ProceduralNoise.fabsInline(this.noise(x / scale, y / scale, z / scale)) * scale;
        }
        return t;
    }

    public differentialTurbulence(x: number, y: number, z: number, octaves: number): Vector3Dd {
        let rx = 0.0;
        let ry = 0.0;
        let rz = 0.0;
        for (let i = 0; i < octaves; i++) {
            const scale: number = Math.pow(0.5, i);
            const value: Vector3Dd = this.differentialNoise(x / scale, y / scale, z / scale);
            rx += value.x() * scale;
            ry += value.y() * scale;
            rz += value.z() * scale;
        }
        return new Vector3Dd(rx, ry, rz);
    }

    public hashTable(): Int16Array | null {
        return this.permutationTable;
    }

    public checksumTable(): LookUpTableChecksum16 {
        return this.checksumLookUpTable;
    }

    private ensureInitialized(): void {
        if (this.permutationTable === null || this.rTable === null) {
            this.initialize();
        }
    }

    private initTextureTable(): void {
        const random = new _CRandom(0);
        const table = new Int16Array(4096);
        for (let i = 0; i < 4096; i++) {
            table[i] = i;
        }
        for (let i = 4095; i >= 0; i--) {
            const j: number = random.next() % 4096;
            const temp: number = table[i]!;
            table[i] = table[j]!;
            table[j] = temp;
        }
        this.permutationTable = table;
    }

    private initRTable(): void {
        this.initTextureTable();
        const table = new Float64Array(ProceduralNoise.MAXSIZE);
        for (let i = 0; i < ProceduralNoise.MAXSIZE; i++) {
            table[i] = (this.checksumVector(new Vector3Dd(i, i, i)) & 0xffff) * ProceduralNoise.REAL_SCALE - 1.0;
        }
        this.rTable = table;
    }

    private checksumVector(v: Vector3Dd): number {
        const bytes = new Uint8Array(24);
        const view = new DataView(bytes.buffer);
        view.setFloat64(0, v.x() * 0.12345, true);
        view.setFloat64(8, v.y() * 0.12345, true);
        view.setFloat64(16, v.z() * 0.12345, true);
        return this.checksumLookUpTable.evalBuffer(bytes, 24);
    }

    private hash3d(a: number, b: number, c: number): number {
        const table: Int16Array = this.permutationTable!;
        const i0: number = a & 0xfff;
        const i1: number = (table[i0]! ^ (b & 0xfff)) & 0xfff;
        const i2: number = (table[i1]! ^ (c & 0xfff)) & 0xfff;
        return table[i2]!;
    }

    private incrSum(m: number, s: number, x: number, y: number, z: number): number {
        const table: Float64Array = this.rTable!;
        return s * (table[m]! * 0.5 + table[m + 1]! * x + table[m + 2]! * y + table[m + 3]! * z);
    }

    private setupLattice(x: number, y: number, z: number): _Lattice {
        const l = new _Lattice();
        l.x = x - ProceduralNoise.MIN_X;
        l.y = y - ProceduralNoise.MIN_Y;
        l.z = z - ProceduralNoise.MIN_Z;
        l.ix = Math.trunc(l.x);
        l.iy = Math.trunc(l.y);
        l.iz = Math.trunc(l.z);
        l.jx = l.ix + 1;
        l.jy = l.iy + 1;
        l.jz = l.iz + 1;
        l.sx = this.sCurve(l.x - l.ix);
        l.sy = this.sCurve(l.y - l.iy);
        l.sz = this.sCurve(l.z - l.iz);
        l.tx = 1.0 - l.sx;
        l.ty = 1.0 - l.sy;
        l.tz = 1.0 - l.sz;
        return l;
    }

    private static fabsInline(x: number): number {
        return x < 0.0 ? 0.0 - x : x;
    }
}
