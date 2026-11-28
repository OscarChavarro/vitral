import type { Image } from "../../../media/Image.js";
import { ArithmeticException } from "../../../../../java/lang/ArithmeticException.js";
import { IndexOutOfBoundsException } from "../../../../../java/lang/IndexOutOfBoundsException.js";
import { _AnimationInfo } from "./_AnimationInfo.js";
import { TriangleMesh } from "./TriangleMesh.js";

/**
In-memory MD2 model data and animation-range metadata.

Java's `ArrayList<...>` fields are JavaScript arrays here, `float[]` /
`short[]` / `int[]` are `Float32Array` / `Int16Array` / `Int32Array`, and the
Java `short` and `float` fields keep their Java value ranges (`toShort`,
`Math.fround`).

@author
*/
export class Md2Mesh {
    public ident = 0; // magic number: "IDP2" (844121161)
    public version = 0; // version: must be 8
    public skinWidth = 0; // texture width
    public skinHeight = 0; // texture height
    public frameSize = 0; // size in bytes of a frame
    public numSkins = 0; // number of skins
    public numVertices = 0; // number of vertices per frame
    public numTexCoords = 0; // number of texture coordinates
    public numTriangles = 0; // number of triangles
    public numGlCommands = 0; // number of opengl commands
    public numFrames = 0; // number of frames
    public offsetSkins = 0; // offset skin data
    public offsetTexCoords = 0; // offset texture coordinate data
    public offsetTriangles = 0; // offset triangle data
    public offsetFrames = 0; // offset frame data
    public offsetGlCommands = 0; // offset OpenGL command data
    public offsetEnd = 0; // offset end of file

    public skinNames: string[] = [];
    public skins: Image[] | null = null;
    public frameNames: string[] = [];
    public frameVertices: Float32Array[] = [];
    public frameNormalIndices: Int16Array[] = [];
    public glCmdTexCoordsStrip: Float32Array[] = [];
    public glCmdVertIndexStrip: Int32Array[] = [];
    public glCmdTexCoordsFan: Float32Array[] = [];
    public glCmdVertIndexFan: Int32Array[] = [];
    public texCoords: Float32Array | null = null;
    public triangles: number[][] | null = null;
    private aniInfos: _AnimationInfo[] = [];
    private frameTimeSeg: number = Math.fround(0.25);
    private elapsedTimeSeg = 0;
    //    private int currentFrame=0;
    private currentAnimationInd = 0;
    private maxAnimationInd = 0;

    /** Java `(short)` narrowing conversion. */
    private static toShort(value: number): number {
        return (value << 16) >> 16;
    }

    public getMaxAnimationInd(): number {
        return this.maxAnimationInd;
    }
    public setMaxAnimationInd(maxAnimationInd: number): void {
        this.maxAnimationInd = Md2Mesh.toShort(maxAnimationInd);
    }
    public getCurrentAnimationInd(): number {
        return this.currentAnimationInd;
    }
    public setCurrentAnimationInd(currentAnimationInd: number): void {
        // Java int remainder: the sign of the dividend; division by zero throws.
        const divisor = this.maxAnimationInd + 1;
        if (divisor === 0) {
            throw new ArithmeticException("/ by zero");
        }
        this.currentAnimationInd = Md2Mesh.toShort(Md2Mesh.toShort(currentAnimationInd) % divisor);
    }
    public getFrameTimeSeg(): number {
        return this.frameTimeSeg;
    }
    public setFrameTimeSeg(frameTimeSeg: number): void {
        this.frameTimeSeg = Math.fround(frameTimeSeg);
    }
    public getElapsedTimeSeg(): number {
        return this.elapsedTimeSeg;
    }
    public setElapsedTimeSeg(elapsedTimeSeg: number): void {
        this.elapsedTimeSeg = Math.fround(elapsedTimeSeg);
    }
    //    public int getCurrentFrame() {
    //        return currentFrame;
    //    }
    //    public void setCurrentFrame(int currentFrame) {
    //        this.currentFrame = currentFrame % numFrames;
    //    }

    //    public int getCurrentFrameInc() {
    //        if(currentFrame == numFrames)// All the animations.
    //            currentFrame = 0;
    //        else
    //            ++currentFrame;
    //        return currentFrame;
    //    };

    /**
    The triangles of a key frame of the model (without interpolation between
    frames), as a triangle mesh with the positions of the frame.
    @param frame index of the frame, from 0 to `frameVertices.size() - 1`
    @return a new mesh with positions and triangles only, or null if the
    model has no such frame or no triangles
    */
    public exportToTriangleMesh(frame: number): TriangleMesh | null {
        if (frame < 0 || frame >= this.frameVertices.length || this.triangles === null) {
            return null;
        }
        const vertices = this.frameVertices[frame]!;
        const mesh = new TriangleMesh();
        const count = Math.min(this.numTriangles, Math.trunc(this.triangles.length / 2));
        let i: number;
        let j: number;

        mesh.initVertexPositionsArray(Math.trunc(vertices.length / 3));
        const positions = mesh.getVertexPositions()!;
        for (i = 0; i < positions.length; i++) {
            positions[i] = vertices[i]!;
        }
        mesh.initTriangleArrays(count);
        const indexes = mesh.getTriangleIndexes()!;
        for (i = 0; i < count; i++) {
            // triangles[2*i] are the vertex indexes, triangles[2*i+1] the
            // texture coordinate indexes
            for (j = 0; j < 3; j++) {
                indexes[3 * i + j] = this.triangles[2 * i]![j]!;
            }
        }
        return mesh;
    }

    /**
    If frameNames is empty return 0 and 0 in outStartEnd array.
    Java overloads `returnStartEndAnim(short, short[])` and
    `returnStartEndAnim(String, short[])`; the index form reads
    `aniInfos.get(inIndex)`, so an index out of range throws.
    */
    public returnStartEndAnim(inIndex: number, outStartEnd: Int16Array | number[]): void;
    /**
    If frameNames is empty or inNameAnim is not found, return 0 and 0 in
    in outStartEnd array.
    */
    public returnStartEndAnim(inNameAnim: string, outStartEnd: Int16Array | number[]): void;
    public returnStartEndAnim(query: number | string, outStartEnd: Int16Array | number[]): void {
        if (this.frameNames.length === 0) {
            outStartEnd[0] = 0;
            outStartEnd[1] = 0;
            return;
        }
        if (this.aniInfos.length === 0) {
            this.fillAniInfo();
        }
        if (typeof query === "number") {
            const info = Md2Mesh.listGet(this.aniInfos, Md2Mesh.toShort(query));
            outStartEnd[0] = info.start;
            outStartEnd[1] = info.end;
            return;
        }
        const ai = this.getAniInfo(query);
        if (ai !== null) {
            outStartEnd[0] = ai.start;
            outStartEnd[1] = ai.end;
        } else {
            outStartEnd[0] = 0;
            outStartEnd[1] = 0;
        }
    }

    public getAnimationName(index: number): string {
        if (this.frameNames.length === 0) {
            return "";
        }
        if (this.aniInfos.length === 0) {
            this.fillAniInfo();
        }
        if (this.aniInfos.length === 0) {
            return "";
        }

        let safeIndex = Md2Mesh.toShort(index);
        if (safeIndex < 0) {
            safeIndex = 0;
        }
        if (safeIndex >= this.aniInfos.length) {
            safeIndex = this.aniInfos.length - 1;
        }
        // fillAniInfo names every entry it adds.
        return this.aniInfos[safeIndex]!.name as string;
    }

    private getAniInfo(nameAnim: string): _AnimationInfo | null {
        for (const ai of this.aniInfos) {
            if (ai.name === Md2Mesh.javaTrim(nameAnim)) return ai;
        }
        return null;
    }

    private fillAniInfo(): void {
        let i: number;
        let pos: number;
        let fName: string;
        let aniInfoT: _AnimationInfo;

        aniInfoT = new _AnimationInfo();
        // aniInfoT.name initially must be different from any frame name,
        // this is guaranteed with a length superior to 16.
        aniInfoT.name = "12345678901234567890";
        for (i = 0; i < this.frameNames.length; i = Md2Mesh.toShort(i + 1)) {
            fName = this.frameNames[i]!;
            fName = Md2Mesh.javaTrim(fName);
            pos = Md2Mesh.toShort(fName.indexOf("\u0000")); // \u0000 = null character.
            if (pos === -1) pos = Md2Mesh.toShort(fName.length);
            fName = fName.substring(0, pos);
            pos--;
            while (pos !== -1 && Md2Mesh.javaIsDigit(fName.charAt(pos))) --pos;
            fName = fName.substring(0, pos + 1);
            if (fName !== aniInfoT.name) {
                if (i !== 0) {
                    aniInfoT.end = Md2Mesh.toShort(i - 1);
                    this.aniInfos.push(aniInfoT);
                }
                aniInfoT = new _AnimationInfo();
                aniInfoT.name = fName;
                aniInfoT.start = i;
            }
        }
        // The last animation(needs the 'i' final value).
        aniInfoT.end = Md2Mesh.toShort(i - 1);
        this.aniInfos.push(aniInfoT);
        this.maxAnimationInd = Md2Mesh.toShort(this.aniInfos.length - 1);
    }

    /** Java `String.trim()`: removes the leading and trailing chars <= ' '. */
    private static javaTrim(text: string): string {
        let start = 0;
        let end = text.length;
        while (start < end && text.charCodeAt(start) <= 0x20) start++;
        while (end > start && text.charCodeAt(end - 1) <= 0x20) end--;
        return text.substring(start, end);
    }

    /** Java `Character.isDigit(char)`: Unicode decimal digits. */
    private static javaIsDigit(c: string): boolean {
        return /^\p{Nd}$/u.test(c);
    }

    /** `ArrayList.get` */
    private static listGet<T>(list: T[], index: number): T {
        if (index < 0 || index >= list.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + list.length);
        }
        return list[index]!;
    }

    /** Java `float[][]`: every value is a float. */
    public static anorms: number[][] = [
        [Math.fround(-0.525731), Math.fround(0.0), Math.fround(0.850651)],
        [Math.fround(-0.442863), Math.fround(0.238856), Math.fround(0.864188)],
        [Math.fround(-0.295242), Math.fround(0.0), Math.fround(0.955423)],
        [Math.fround(-0.309017), Math.fround(0.5), Math.fround(0.809017)],
        [Math.fround(-0.16246), Math.fround(0.262866), Math.fround(0.951056)],
        [Math.fround(0.0), Math.fround(0.0), Math.fround(1.0)],
        [Math.fround(0.0), Math.fround(0.850651), Math.fround(0.525731)],
        [Math.fround(-0.147621), Math.fround(0.716567), Math.fround(0.681718)],
        [Math.fround(0.147621), Math.fround(0.716567), Math.fround(0.681718)],
        [Math.fround(0.0), Math.fround(0.525731), Math.fround(0.850651)],
        [Math.fround(0.309017), Math.fround(0.5), Math.fround(0.809017)],
        [Math.fround(0.525731), Math.fround(0.0), Math.fround(0.850651)],
        [Math.fround(0.295242), Math.fround(0.0), Math.fround(0.955423)],
        [Math.fround(0.442863), Math.fround(0.238856), Math.fround(0.864188)],
        [Math.fround(0.16246), Math.fround(0.262866), Math.fround(0.951056)],
        [Math.fround(-0.681718), Math.fround(0.147621), Math.fround(0.716567)],
        [Math.fround(-0.809017), Math.fround(0.309017), Math.fround(0.5)],
        [Math.fround(-0.587785), Math.fround(0.425325), Math.fround(0.688191)],
        [Math.fround(-0.850651), Math.fround(0.525731), Math.fround(0.0)],
        [Math.fround(-0.864188), Math.fround(0.442863), Math.fround(0.238856)],
        [Math.fround(-0.716567), Math.fround(0.681718), Math.fround(0.147621)],
        [Math.fround(-0.688191), Math.fround(0.587785), Math.fround(0.425325)],
        [Math.fround(-0.5), Math.fround(0.809017), Math.fround(0.309017)],
        [Math.fround(-0.238856), Math.fround(0.864188), Math.fround(0.442863)],
        [Math.fround(-0.425325), Math.fround(0.688191), Math.fround(0.587785)],
        [Math.fround(-0.716567), Math.fround(0.681718), Math.fround(-0.147621)],
        [Math.fround(-0.5), Math.fround(0.809017), Math.fround(-0.309017)],
        [Math.fround(-0.525731), Math.fround(0.850651), Math.fround(0.0)],
        [Math.fround(0.0), Math.fround(0.850651), Math.fround(-0.525731)],
        [Math.fround(-0.238856), Math.fround(0.864188), Math.fround(-0.442863)],
        [Math.fround(0.0), Math.fround(0.955423), Math.fround(-0.295242)],
        [Math.fround(-0.262866), Math.fround(0.951056), Math.fround(-0.16246)],
        [Math.fround(0.0), Math.fround(1.0), Math.fround(0.0)],
        [Math.fround(0.0), Math.fround(0.955423), Math.fround(0.295242)],
        [Math.fround(-0.262866), Math.fround(0.951056), Math.fround(0.16246)],
        [Math.fround(0.238856), Math.fround(0.864188), Math.fround(0.442863)],
        [Math.fround(0.262866), Math.fround(0.951056), Math.fround(0.16246)],
        [Math.fround(0.5), Math.fround(0.809017), Math.fround(0.309017)],
        [Math.fround(0.238856), Math.fround(0.864188), Math.fround(-0.442863)],
        [Math.fround(0.262866), Math.fround(0.951056), Math.fround(-0.16246)],
        [Math.fround(0.5), Math.fround(0.809017), Math.fround(-0.309017)],
        [Math.fround(0.850651), Math.fround(0.525731), Math.fround(0.0)],
        [Math.fround(0.716567), Math.fround(0.681718), Math.fround(0.147621)],
        [Math.fround(0.716567), Math.fround(0.681718), Math.fround(-0.147621)],
        [Math.fround(0.525731), Math.fround(0.850651), Math.fround(0.0)],
        [Math.fround(0.425325), Math.fround(0.688191), Math.fround(0.587785)],
        [Math.fround(0.864188), Math.fround(0.442863), Math.fround(0.238856)],
        [Math.fround(0.688191), Math.fround(0.587785), Math.fround(0.425325)],
        [Math.fround(0.809017), Math.fround(0.309017), Math.fround(0.5)],
        [Math.fround(0.681718), Math.fround(0.147621), Math.fround(0.716567)],
        [Math.fround(0.587785), Math.fround(0.425325), Math.fround(0.688191)],
        [Math.fround(0.955423), Math.fround(0.295242), Math.fround(0.0)],
        [Math.fround(1.0), Math.fround(0.0), Math.fround(0.0)],
        [Math.fround(0.951056), Math.fround(0.16246), Math.fround(0.262866)],
        [Math.fround(0.850651), Math.fround(-0.525731), Math.fround(0.0)],
        [Math.fround(0.955423), Math.fround(-0.295242), Math.fround(0.0)],
        [Math.fround(0.864188), Math.fround(-0.442863), Math.fround(0.238856)],
        [Math.fround(0.951056), Math.fround(-0.16246), Math.fround(0.262866)],
        [Math.fround(0.809017), Math.fround(-0.309017), Math.fround(0.5)],
        [Math.fround(0.681718), Math.fround(-0.147621), Math.fround(0.716567)],
        [Math.fround(0.850651), Math.fround(0.0), Math.fround(0.525731)],
        [Math.fround(0.864188), Math.fround(0.442863), Math.fround(-0.238856)],
        [Math.fround(0.809017), Math.fround(0.309017), Math.fround(-0.5)],
        [Math.fround(0.951056), Math.fround(0.16246), Math.fround(-0.262866)],
        [Math.fround(0.525731), Math.fround(0.0), Math.fround(-0.850651)],
        [Math.fround(0.681718), Math.fround(0.147621), Math.fround(-0.716567)],
        [Math.fround(0.681718), Math.fround(-0.147621), Math.fround(-0.716567)],
        [Math.fround(0.850651), Math.fround(0.0), Math.fround(-0.525731)],
        [Math.fround(0.809017), Math.fround(-0.309017), Math.fround(-0.5)],
        [Math.fround(0.864188), Math.fround(-0.442863), Math.fround(-0.238856)],
        [Math.fround(0.951056), Math.fround(-0.16246), Math.fround(-0.262866)],
        [Math.fround(0.147621), Math.fround(0.716567), Math.fround(-0.681718)],
        [Math.fround(0.309017), Math.fround(0.5), Math.fround(-0.809017)],
        [Math.fround(0.425325), Math.fround(0.688191), Math.fround(-0.587785)],
        [Math.fround(0.442863), Math.fround(0.238856), Math.fround(-0.864188)],
        [Math.fround(0.587785), Math.fround(0.425325), Math.fround(-0.688191)],
        [Math.fround(0.688191), Math.fround(0.587785), Math.fround(-0.425325)],
        [Math.fround(-0.147621), Math.fround(0.716567), Math.fround(-0.681718)],
        [Math.fround(-0.309017), Math.fround(0.5), Math.fround(-0.809017)],
        [Math.fround(0.0), Math.fround(0.525731), Math.fround(-0.850651)],
        [Math.fround(-0.525731), Math.fround(0.0), Math.fround(-0.850651)],
        [Math.fround(-0.442863), Math.fround(0.238856), Math.fround(-0.864188)],
        [Math.fround(-0.295242), Math.fround(0.0), Math.fround(-0.955423)],
        [Math.fround(-0.16246), Math.fround(0.262866), Math.fround(-0.951056)],
        [Math.fround(0.0), Math.fround(0.0), Math.fround(-1.0)],
        [Math.fround(0.295242), Math.fround(0.0), Math.fround(-0.955423)],
        [Math.fround(0.16246), Math.fround(0.262866), Math.fround(-0.951056)],
        [Math.fround(-0.442863), Math.fround(-0.238856), Math.fround(-0.864188)],
        [Math.fround(-0.309017), Math.fround(-0.5), Math.fround(-0.809017)],
        [Math.fround(-0.16246), Math.fround(-0.262866), Math.fround(-0.951056)],
        [Math.fround(0.0), Math.fround(-0.850651), Math.fround(-0.525731)],
        [Math.fround(-0.147621), Math.fround(-0.716567), Math.fround(-0.681718)],
        [Math.fround(0.147621), Math.fround(-0.716567), Math.fround(-0.681718)],
        [Math.fround(0.0), Math.fround(-0.525731), Math.fround(-0.850651)],
        [Math.fround(0.309017), Math.fround(-0.5), Math.fround(-0.809017)],
        [Math.fround(0.442863), Math.fround(-0.238856), Math.fround(-0.864188)],
        [Math.fround(0.16246), Math.fround(-0.262866), Math.fround(-0.951056)],
        [Math.fround(0.238856), Math.fround(-0.864188), Math.fround(-0.442863)],
        [Math.fround(0.5), Math.fround(-0.809017), Math.fround(-0.309017)],
        [Math.fround(0.425325), Math.fround(-0.688191), Math.fround(-0.587785)],
        [Math.fround(0.716567), Math.fround(-0.681718), Math.fround(-0.147621)],
        [Math.fround(0.688191), Math.fround(-0.587785), Math.fround(-0.425325)],
        [Math.fround(0.587785), Math.fround(-0.425325), Math.fround(-0.688191)],
        [Math.fround(0.0), Math.fround(-0.955423), Math.fround(-0.295242)],
        [Math.fround(0.0), Math.fround(-1.0), Math.fround(0.0)],
        [Math.fround(0.262866), Math.fround(-0.951056), Math.fround(-0.16246)],
        [Math.fround(0.0), Math.fround(-0.850651), Math.fround(0.525731)],
        [Math.fround(0.0), Math.fround(-0.955423), Math.fround(0.295242)],
        [Math.fround(0.238856), Math.fround(-0.864188), Math.fround(0.442863)],
        [Math.fround(0.262866), Math.fround(-0.951056), Math.fround(0.16246)],
        [Math.fround(0.5), Math.fround(-0.809017), Math.fround(0.309017)],
        [Math.fround(0.716567), Math.fround(-0.681718), Math.fround(0.147621)],
        [Math.fround(0.525731), Math.fround(-0.850651), Math.fround(0.0)],
        [Math.fround(-0.238856), Math.fround(-0.864188), Math.fround(-0.442863)],
        [Math.fround(-0.5), Math.fround(-0.809017), Math.fround(-0.309017)],
        [Math.fround(-0.262866), Math.fround(-0.951056), Math.fround(-0.16246)],
        [Math.fround(-0.850651), Math.fround(-0.525731), Math.fround(0.0)],
        [Math.fround(-0.716567), Math.fround(-0.681718), Math.fround(-0.147621)],
        [Math.fround(-0.716567), Math.fround(-0.681718), Math.fround(0.147621)],
        [Math.fround(-0.525731), Math.fround(-0.850651), Math.fround(0.0)],
        [Math.fround(-0.5), Math.fround(-0.809017), Math.fround(0.309017)],
        [Math.fround(-0.238856), Math.fround(-0.864188), Math.fround(0.442863)],
        [Math.fround(-0.262866), Math.fround(-0.951056), Math.fround(0.16246)],
        [Math.fround(-0.864188), Math.fround(-0.442863), Math.fround(0.238856)],
        [Math.fround(-0.809017), Math.fround(-0.309017), Math.fround(0.5)],
        [Math.fround(-0.688191), Math.fround(-0.587785), Math.fround(0.425325)],
        [Math.fround(-0.681718), Math.fround(-0.147621), Math.fround(0.716567)],
        [Math.fround(-0.442863), Math.fround(-0.238856), Math.fround(0.864188)],
        [Math.fround(-0.587785), Math.fround(-0.425325), Math.fround(0.688191)],
        [Math.fround(-0.309017), Math.fround(-0.5), Math.fround(0.809017)],
        [Math.fround(-0.147621), Math.fround(-0.716567), Math.fround(0.681718)],
        [Math.fround(-0.425325), Math.fround(-0.688191), Math.fround(0.587785)],
        [Math.fround(-0.16246), Math.fround(-0.262866), Math.fround(0.951056)],
        [Math.fround(0.442863), Math.fround(-0.238856), Math.fround(0.864188)],
        [Math.fround(0.16246), Math.fround(-0.262866), Math.fround(0.951056)],
        [Math.fround(0.309017), Math.fround(-0.5), Math.fround(0.809017)],
        [Math.fround(0.147621), Math.fround(-0.716567), Math.fround(0.681718)],
        [Math.fround(0.0), Math.fround(-0.525731), Math.fround(0.850651)],
        [Math.fround(0.425325), Math.fround(-0.688191), Math.fround(0.587785)],
        [Math.fround(0.587785), Math.fround(-0.425325), Math.fround(0.688191)],
        [Math.fround(0.688191), Math.fround(-0.587785), Math.fround(0.425325)],
        [Math.fround(-0.955423), Math.fround(0.295242), Math.fround(0.0)],
        [Math.fround(-0.951056), Math.fround(0.16246), Math.fround(0.262866)],
        [Math.fround(-1.0), Math.fround(0.0), Math.fround(0.0)],
        [Math.fround(-0.850651), Math.fround(0.0), Math.fround(0.525731)],
        [Math.fround(-0.955423), Math.fround(-0.295242), Math.fround(0.0)],
        [Math.fround(-0.951056), Math.fround(-0.16246), Math.fround(0.262866)],
        [Math.fround(-0.864188), Math.fround(0.442863), Math.fround(-0.238856)],
        [Math.fround(-0.951056), Math.fround(0.16246), Math.fround(-0.262866)],
        [Math.fround(-0.809017), Math.fround(0.309017), Math.fround(-0.5)],
        [Math.fround(-0.864188), Math.fround(-0.442863), Math.fround(-0.238856)],
        [Math.fround(-0.951056), Math.fround(-0.16246), Math.fround(-0.262866)],
        [Math.fround(-0.809017), Math.fround(-0.309017), Math.fround(-0.5)],
        [Math.fround(-0.681718), Math.fround(0.147621), Math.fround(-0.716567)],
        [Math.fround(-0.681718), Math.fround(-0.147621), Math.fround(-0.716567)],
        [Math.fround(-0.850651), Math.fround(0.0), Math.fround(-0.525731)],
        [Math.fround(-0.688191), Math.fround(0.587785), Math.fround(-0.425325)],
        [Math.fround(-0.587785), Math.fround(0.425325), Math.fround(-0.688191)],
        [Math.fround(-0.425325), Math.fround(0.688191), Math.fround(-0.587785)],
        [Math.fround(-0.425325), Math.fround(-0.688191), Math.fround(-0.587785)],
        [Math.fround(-0.587785), Math.fround(-0.425325), Math.fround(-0.688191)],
        [Math.fround(-0.688191), Math.fround(-0.587785), Math.fround(-0.425325)],
    ];
}
