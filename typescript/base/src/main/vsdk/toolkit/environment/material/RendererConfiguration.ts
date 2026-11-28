import { FundamentalEntity } from "../../common/FundamentalEntity.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Double } from "../../../../java/lang/Double.js";
import { ShadingType } from "./ShadingType.js";
import { TriangleMeshAccelerationMethod } from "../geometry/boundingVolumeHierarchy/TriangleMeshAccelerationMethod.js";
import { TriangleMeshAccelerationMethodUtil } from "../geometry/boundingVolumeHierarchy/TriangleMeshAccelerationMethodUtil.js";

/**
The RendererConfiguration class is used to indicate some attributes in which a
geometry is to be displayed in screen. The RendererConfiguration class is not
responsible of display any data, it is just an a suggestion on how to draw it.

\todo : Rename to 'RenderConfiguration' or something similar...

Java's two `clone` methods share one name here: `clone()` returns a copy and
`clone(other)` copies `other` into this object.
*/
export class RendererConfiguration extends FundamentalEntity {
    public static readonly SHADING_TYPE_NOLIGHT = 0;
    public static readonly SHADING_TYPE_FLAT = 1;
    public static readonly SHADING_TYPE_GOURAUD = 2;
    public static readonly SHADING_TYPE_PHONG = 3;
    public static readonly SHADING_TYPE_COOK_TERRANCE = 4;

    private shadingType: number;
    private surfaces: boolean;
    private wires: boolean;
    private boundingVolume: boolean;
    private selectionCorners: boolean;
    private texture: boolean;
    private bumpMap: boolean;
    private points: boolean;
    private normals: boolean;
    private trianglesNormals: boolean;
    private withShadows: boolean;
    private useVertexColors: boolean;
    private vertexNormalSmoothingThresholdDegrees: number;
    private wireColor: ColorRgb;
    private boundingVolumeColor: ColorRgb;
    private meshAccelerationMethod: TriangleMeshAccelerationMethod;
    private lodHint = 0;

    public compareTo(other: RendererConfiguration): number {
        let valThis: number;
        let valOther: number;

        valThis = 0x00;
        if (this.surfaces) valThis += 0x0001;
        if (this.wires) valThis += 0x0002;
        if (this.boundingVolume) valThis += 0x0004;
        if (this.selectionCorners) valThis += 0x0008;
        if (this.texture) valThis += 0x0010;
        if (this.bumpMap) valThis += 0x0020;
        if (this.points) valThis += 0x0040;
        if (this.normals) valThis += 0x0080;
        if (this.trianglesNormals) valThis += 0x0100;
        if (this.withShadows) valThis += 0x0200;
        valThis += Math.imul(this.shadingType, 0x1000);
        valThis += Math.round(this.vertexNormalSmoothingThresholdDegrees) * 0x100000;

        valOther = 0x00;
        if (other.surfaces) valOther += 0x0001;
        if (other.wires) valOther += 0x0002;
        if (other.boundingVolume) valOther += 0x0004;
        if (other.selectionCorners) valOther += 0x0008;
        if (other.texture) valOther += 0x0010;
        if (other.bumpMap) valOther += 0x0020;
        if (other.points) valOther += 0x0040;
        if (other.normals) valOther += 0x0080;
        if (other.trianglesNormals) valOther += 0x0100;
        if (other.withShadows) valOther += 0x0200;
        valOther += Math.imul(other.shadingType, 0x1000);
        valOther += Math.round(other.vertexNormalSmoothingThresholdDegrees) * 0x100000;

        if (valThis > valOther) {
            return 1;
        } else if (valThis < valOther) {
            return -1;
        }
        return 0;
    }

    /**
    Constructs a default RendererConfiguration object that will display the surfaces
    */
    public constructor() {
        super();
        this.shadingType = RendererConfiguration.SHADING_TYPE_GOURAUD;
        this.surfaces = true;
        this.wires = false;
        this.boundingVolume = false;
        this.selectionCorners = false;
        this.texture = true;
        this.bumpMap = false;
        this.points = false;
        this.normals = false;
        this.trianglesNormals = false;
        this.withShadows = false;
        this.wireColor = new ColorRgb(1, 1, 1);
        this.boundingVolumeColor = new ColorRgb(1, 1, 0);
        this.useVertexColors = false;
        this.vertexNormalSmoothingThresholdDegrees = 15.0;
        this.meshAccelerationMethod = TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH;
    }

    /**
    `clone()`: a new object containing an exact copy of current
    RendererConfiguration. `clone(other)`: copies every attribute of `other`
    except the LOD hint into this object, as Java's `clone(RendererConfiguration)`.
    */
    public override clone(): RendererConfiguration;
    public override clone(other: RendererConfiguration): void;
    public override clone(other?: RendererConfiguration): RendererConfiguration | void {
        if (other !== undefined) {
            this.shadingType = other.shadingType;
            this.surfaces = other.surfaces;
            this.wires = other.wires;
            this.boundingVolume = other.boundingVolume;
            this.selectionCorners = other.selectionCorners;
            this.texture = other.texture;
            this.bumpMap = other.bumpMap;
            this.points = other.points;
            this.normals = other.normals;
            this.trianglesNormals = other.trianglesNormals;
            this.withShadows = other.withShadows;
            this.wireColor = new ColorRgb(other.wireColor);
            this.boundingVolumeColor = new ColorRgb(other.boundingVolumeColor);
            this.useVertexColors = other.useVertexColors;
            this.vertexNormalSmoothingThresholdDegrees = other.vertexNormalSmoothingThresholdDegrees;
            this.meshAccelerationMethod = other.meshAccelerationMethod;
            return;
        }

        const copy: RendererConfiguration = new RendererConfiguration();
        copy.shadingType = this.shadingType;
        copy.surfaces = this.surfaces;
        copy.wires = this.wires;
        copy.boundingVolume = this.boundingVolume;
        copy.selectionCorners = this.selectionCorners;
        copy.texture = this.texture;
        copy.bumpMap = this.bumpMap;
        copy.points = this.points;
        copy.normals = this.normals;
        copy.trianglesNormals = this.trianglesNormals;
        copy.withShadows = this.withShadows;
        copy.lodHint = this.lodHint;
        copy.wireColor = new ColorRgb(this.wireColor);
        copy.boundingVolumeColor = new ColorRgb(this.boundingVolumeColor);
        copy.useVertexColors = this.useVertexColors;
        copy.vertexNormalSmoothingThresholdDegrees = this.vertexNormalSmoothingThresholdDegrees;
        copy.meshAccelerationMethod = this.meshAccelerationMethod;
        return copy;
    }

    public setLodHint(l: number): void {
        this.lodHint = l;
    }

    public getLodHint(): number {
        return this.lodHint;
    }

    public setWireColor(c: ColorRgb): void;
    public setWireColor(r: number, g: number, b: number): void;
    public setWireColor(a: ColorRgb | number, g?: number, b?: number): void {
        if (a instanceof ColorRgb) {
            this.wireColor = new ColorRgb(a);
        } else {
            this.wireColor = new ColorRgb(a, g!, b!);
        }
    }

    public getWireColor(): ColorRgb {
        return this.wireColor;
    }

    /**
    Selects wether or not to display the surface of the object
    @param b The selection value, true indicates that the quality should be
    displayed and false indicates that it shouldn't
    */
    public setSurfaces(b: boolean): void {
        this.surfaces = b;
    }

    public setWires(b: boolean): void {
        this.wires = b;
    }

    public setBoundingVolume(b: boolean): void {
        this.boundingVolume = b;
    }

    public setSelectionCorners(c: boolean): void {
        this.selectionCorners = c;
    }

    public setTexture(b: boolean): void {
        this.texture = b;
    }

    public setBumpMap(b: boolean): void {
        this.bumpMap = b;
    }

    public setPoints(b: boolean): void {
        this.points = b;
    }

    public setNormals(b: boolean): void {
        this.normals = b;
    }

    public setTrianglesNormals(b: boolean): void {
        this.trianglesNormals = b;
    }

    /**
    Selects wether or not the renderers that compute shadows as an option
    cast them (i.e. the hidden surface removal of Weiler and Atherton)
    @param b true to cast shadows
    */
    public setWithShadows(b: boolean): void {
        this.withShadows = b;
    }

    /**
    Java's `setShadingType(int)` and `setShadingType(ShadingType)`. A
    `ShadingType` is a number here, so both store its code; null selects
    Gouraud, as the enum overload does.
    */
    public setShadingType(shadingType: number | ShadingType | null): void {
        if (shadingType === null) {
            this.shadingType = RendererConfiguration.SHADING_TYPE_GOURAUD;
            return;
        }
        this.shadingType = shadingType;
    }

    public setVertexNormalSmoothingThresholdDegrees(thresholdDegrees: number): void {
        if (Number.isNaN(thresholdDegrees)) {
            this.vertexNormalSmoothingThresholdDegrees = 15.0;
            return;
        }
        if (thresholdDegrees < 0.0) {
            thresholdDegrees = 0.0;
        }
        if (thresholdDegrees > 180.0) {
            thresholdDegrees = 180.0;
        }
        this.vertexNormalSmoothingThresholdDegrees = thresholdDegrees;
    }

    public isSurfacesSet(): boolean {
        return this.surfaces;
    }

    public isWiresSet(): boolean {
        return this.wires;
    }

    public isBoundingVolumeSet(): boolean {
        return this.boundingVolume;
    }

    public isSelectionCornersSet(): boolean {
        return this.selectionCorners;
    }

    public isTextureSet(): boolean {
        return this.texture;
    }

    public isBumpMapSet(): boolean {
        return this.bumpMap;
    }

    public isPointsSet(): boolean {
        return this.points;
    }

    public isNormalsSet(): boolean {
        return this.normals;
    }

    public isTrianglesNormalsSet(): boolean {
        return this.trianglesNormals;
    }

    public isWithShadowsSet(): boolean {
        return this.withShadows;
    }

    public getShadingType(): number {
        return this.shadingType;
    }

    public getVertexNormalSmoothingThresholdDegrees(): number {
        return this.vertexNormalSmoothingThresholdDegrees;
    }

    public getShadingTypeEnum(): ShadingType {
        return ShadingType.fromCode(this.shadingType);
    }

    public changeSurfaces(): void {
        this.surfaces = !this.surfaces;
    }

    public changeWires(): void {
        this.wires = !this.wires;
    }

    public changeBoundingVolume(): void {
        this.boundingVolume = !this.boundingVolume;
    }

    public changeSelectionCorners(): void {
        this.selectionCorners = !this.selectionCorners;
    }

    public changeTexture(): void {
        this.texture = !this.texture;
    }

    public changeBumpMap(): void {
        this.bumpMap = !this.bumpMap;
    }

    public changePoints(): void {
        this.points = !this.points;
    }

    public changeNormals(): void {
        this.normals = !this.normals;
    }

    public changeTrianglesNormals(): void {
        this.trianglesNormals = !this.trianglesNormals;
    }

    public changeWithShadows(): void {
        this.withShadows = !this.withShadows;
    }

    public changeShadingType(): void {
        this.setShadingType(ShadingType.next(this.getShadingTypeEnum()));
    }

    public override toString(): string {
        let msg: string;

        msg = "<RendererConfiguration>:\n";
        msg = msg + "  - Shading type: ";
        switch (this.shadingType) {
            case RendererConfiguration.SHADING_TYPE_NOLIGHT:
                msg = msg + "LIGHTING DISABLED (ONLY AMBIENT COLOR)\n";
                break;
            case RendererConfiguration.SHADING_TYPE_FLAT:
                msg = msg + "FLAT\n";
                break;
            case RendererConfiguration.SHADING_TYPE_GOURAUD:
                msg = msg + "GOURAUD\n";
                break;
            case RendererConfiguration.SHADING_TYPE_PHONG:
                msg = msg + "PHONG\n";
                break;
            case RendererConfiguration.SHADING_TYPE_COOK_TERRANCE:
                msg = msg + "COOK-TERRANCE\n";
                break;
            default:
                msg = msg + "INVALID!\n";
                break;
        }
        msg = msg + "  - Draw points: " + (this.points ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw wires: " + (this.wires ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw surfaces: " + (this.surfaces ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw bounding volume: " + (this.boundingVolume ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw selection corners: " + (this.selectionCorners ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw normals: " + (this.normals ? "ON" : "OFF") + "\n";
        msg = msg + "  - Draw triangles normals: " + (this.trianglesNormals ? "ON" : "OFF") + "\n";
        msg = msg + "  - With texture: " + (this.texture ? "ON" : "OFF") + "\n";
        msg = msg + "  - With bump map: " + (this.bumpMap ? "ON" : "OFF") + "\n";
        msg = msg + "  - With shadows: " + (this.withShadows ? "ON" : "OFF") + "\n";
        msg =
            msg +
            "  - Vertex normal smoothing threshold: " +
            Double.toString(this.vertexNormalSmoothingThresholdDegrees) +
            " deg\n";
        msg =
            msg +
            "  - Mesh acceleration method: " +
            TriangleMeshAccelerationMethodUtil.name(this.meshAccelerationMethod) +
            "\n";
        return msg;
    }

    /**
    @return the useVertexColors
    */
    public getUseVertexColors(): boolean {
        return this.useVertexColors;
    }

    /**
    @param useVertexColors the useVertexColors to set
    */
    public setUseVertexColors(useVertexColors: boolean): void {
        this.useVertexColors = useVertexColors;
    }

    /**
    @return the boundingVolumeColor
    */
    public getBoundingVolumeColor(): ColorRgb {
        return this.boundingVolumeColor;
    }

    /**
    Method used by the raytracers to intersect rays with triangle meshes
    (default `WALD2007_BINNED_SAH`). It changes only the speed: every method
    gives the same image.
    @param method acceleration method
    */
    public setMeshAccelerationMethod(method: TriangleMeshAccelerationMethod): void {
        this.meshAccelerationMethod = method;
    }

    public getMeshAccelerationMethod(): TriangleMeshAccelerationMethod {
        return this.meshAccelerationMethod;
    }

    /**
    @param boundingVolumeColor the boundingVolumeColor to set
    */
    public setBoundingVolumeColor(boundingVolumeColor: ColorRgb): void {
        this.boundingVolumeColor = boundingVolumeColor;
    }
}
