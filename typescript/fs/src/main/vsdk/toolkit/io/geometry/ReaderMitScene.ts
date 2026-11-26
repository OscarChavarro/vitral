/***************************************************
 *   An instructional Ray-Tracing Renderer written
 *   for MIT 6.837  Fall '98 by Leonard McMillan.
 *   Modified by Tomas Lozano-Perez for Fall '01
 *   Modified by Oscar Chavarro for Spring '04
 *   FUSM 05061.
 *   Modified by Oscar Chavarro for PUJ Vitral
 *   VSDK '05, '06, '10
 ****************************************************/
//===========================================================================

/**
Deep module specifiers rather than the `@vitral/base` barrel: this reader is
loaded inside every worker thread of a parallel render, and the barrel would
make each of them compile the whole library. Measured on a 72-core host, 72
workers booted in 19.7 s through the barrel and in 1.3 s through these
specifiers.
*/

// Java classes
import type { InputStream } from "@vitral/base/java/io/InputStream";
import { ArrayList } from "@vitral/base/java/util/ArrayList";
import { Double } from "@vitral/base/java/lang/Double";

// VSDK classes
import { VSDKFatalException } from "@vitral/base/vsdk/toolkit/common/VSDKFatalException";
import { ColorRgb } from "@vitral/base/vsdk/toolkit/common/color/ColorRgb";
import { Matrix4x4d } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Matrix4x4d";
import { Vector2Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector2Dd";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { Logger } from "@vitral/base/vsdk/toolkit/common/logging/Logger";
import { CubemapBackground } from "@vitral/base/vsdk/toolkit/environment/background/CubemapBackground";
import { SimpleBackground } from "@vitral/base/vsdk/toolkit/environment/background/SimpleBackground";
import { Camera } from "@vitral/base/vsdk/toolkit/environment/camera/Camera";
import { PolyhedralBoundedSolidModeler } from "@vitral/base/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler";
import { SimpleTestGeometryLibrary } from "@vitral/base/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary";
import { BezierPatchSubdivision } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/BezierPatchSubdivision";
import { SubdividedBezierPatch } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch";
import { BlobComponent } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/BlobComponent";
import { BlobSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/BlobSolid";
import { Box } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Box";
import { Cone } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Cone";
import type { HeightFieldSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/HeightFieldSolid";
import { PlanarHalfSpace } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace";
import { PolynomialSurface } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/PolynomialSurface";
import { QuadricSurface } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/QuadricSurface";
import { Sphere } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Sphere";
import { Prism } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Prism";
import { Torus } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Torus";
import type { PolyhedralBoundedSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid";
import { AmbientLight } from "@vitral/base/vsdk/toolkit/environment/light/AmbientLight";
import { DirectionalLight } from "@vitral/base/vsdk/toolkit/environment/light/DirectionalLight";
import { PointLight } from "@vitral/base/vsdk/toolkit/environment/light/PointLight";
import { SimpleMaterial } from "@vitral/base/vsdk/toolkit/environment/material/SimpleMaterial";
import { AffinePlacementStatus } from "@vitral/base/vsdk/toolkit/environment/scene/AffinePlacementStatus";
import { SimpleBody } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleBody";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { CsgBody } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBody";
import { CsgBodyEvaluator } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator";
import { CsgOperation } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgOperation";
import { _CScanf } from "@vitral/base/vsdk/toolkit/io/geometry/_CScanf";
import { ReaderMitQuadGroup } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderMitQuadGroup";
import { ReaderMitTriangleGroup } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderMitTriangleGroup";
import { ImagePersistence } from "@vitral/base/vsdk/toolkit/io/image/ImagePersistence";
import type { Image } from "@vitral/base/vsdk/toolkit/media/Image";
import type { IndexedColorImageUncompressed } from "@vitral/base/vsdk/toolkit/media/IndexedColorImageUncompressed";
import { NormalMap } from "@vitral/base/vsdk/toolkit/media/NormalMap";
import type { RGBAImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBAImageUncompressed";
import type { RGBImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBImageUncompressed";

import { File } from "../../../../java/io/File.js";
import { FileInputStream } from "../../../../java/io/FileInputStream.js";
import { ImagePersistenceRasterImport } from "../image/ImagePersistenceRasterImport.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { EnvironmentPersistence } from "./EnvironmentPersistence.js";
import { ReaderAltimetry } from "./ReaderAltimetry.js";

/** The cpp `hasPlacement` and `placement` locals, shared with `place`. */
class _PendingPlacement {
    public hasPlacement = false;
    public placement = new Matrix4x4d();
}

/**
Reader of the MIT 6.837 scene format as extended by VITRAL. Relative resource
files (textures, bump maps, altimetry, OBJ models, cubemap faces) are looked
up from the scene file's directory first and then from the working directory.
Textures and normal maps are scene resources shared by the bodies using them.

This class implements an scene reader based on the instructional raytracer
from Tomas Lozano-Perez from computer graphics class at MIT on spring 2001,
and from Leonard McMillan 1998. This material was adapted by Oscar Chavarro
for computer graphics classes at Colombia, and later as anoter scene reader
for Vitral.

The parsing follows the cpp reader line by line: each line is read as C
`fgets` with a 1024 byte buffer would, and its fields are scanned with the C
`sscanf` semantics of `_CScanf`.

Raster RGB and RGBA image import is not part of the TypeScript port yet
(`@vitral/fs`'s `ImagePersistence` only exports): `texture` and
`backgroundcubemap` therefore fail as the cpp reader does for an unreadable
file. Bump maps are read through `ImagePersistence.importIndexedColor`.
*/
export class ReaderMitScene extends PersistenceElement {
    private sceneDirectory: string;
    // Euler angles of the last `rotation` line, applied to each later primitive.
    private yaw: number;
    private pitch: number;
    private roll: number;
    private currentTexture: Image | null;
    private currentNormalMap: NormalMap | null;
    private readonly texturePaths = new ArrayList<string>();
    private readonly textures = new ArrayList<Image>();
    private readonly normalMapKeys = new ArrayList<string>();
    private readonly normalMaps = new ArrayList<NormalMap>();

    public constructor() {
        super();
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;
        this.currentTexture = null;
        this.currentNormalMap = null;
        this.sceneDirectory = "";
    }

    private resetState(fileName: string): void {
        const slash = fileName.lastIndexOf("/");
        this.sceneDirectory = slash < 0 ? "" : fileName.substring(0, slash + 1);
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;
        this.currentTexture = null;
        this.currentNormalMap = null;
        this.texturePaths.clear();
        this.textures.clear();
        this.normalMapKeys.clear();
        this.normalMaps.clear();
    }

    private hasRotation(): boolean {
        return this.yaw !== 0 || this.pitch !== 0 || this.roll !== 0;
    }

    /** cpp `fopen(path, "rb")` succeeding. */
    private static canOpen(path: string): boolean {
        return new File(path).canRead();
    }

    /** Resolved path of a relative resource, or the path as given when missing. */
    private resolveResource(path: string): string {
        if (path.length === 0 || path.charAt(0) === "/" || this.sceneDirectory.length === 0) return path;
        const fromScene = this.sceneDirectory + path;
        if (ReaderMitScene.canOpen(fromScene)) {
            return fromScene;
        }
        return path;
    }

    // Whole contents of a local file.
    private static readFile(path: string): Uint8Array {
        const stream = new FileInputStream(path);
        try {
            return stream.readAllBytes();
        } finally {
            stream.close();
        }
    }

    // cpp `ImagePersistence::importRGB`
    private static importRGB(path: string): RGBImageUncompressed | null {
        return ImagePersistenceRasterImport.importRGB(path);
    }

    // cpp `ImagePersistence::importRGBA`
    private static importRGBA(path: string): RGBAImageUncompressed | null {
        return ImagePersistenceRasterImport.importRGBA(path);
    }

    private loadTexture(path: string, scene: SimpleScene): Image | null {
        for (let i = 0; i < this.texturePaths.size(); ++i)
            if (this.texturePaths.get(i) === path) return this.textures.get(i);
        let texture: RGBImageUncompressed | null;
        try {
            texture = ReaderMitScene.importRGB(this.resolveResource(path));
        } catch {
            texture = null;
        }
        // Unreadable files come back as empty images.
        if (texture !== null && (texture.getXSize() <= 0 || texture.getYSize() <= 0)) {
            texture = null;
        }
        if (texture === null) return null;
        scene.addImage(texture);
        this.texturePaths.add(path);
        this.textures.add(texture);
        return texture;
    }

    private loadNormalMap(path: string, scale: Vector3Dd, scene: SimpleScene): NormalMap | null {
        // The cpp key formats the scale with "%.17g"; any exact (round trip)
        // formatting identifies the same scales.
        const key =
            path +
            "|" +
            Double.toString(scale.x()) +
            "|" +
            Double.toString(scale.y()) +
            "|" +
            Double.toString(scale.z());
        for (let i = 0; i < this.normalMapKeys.size(); ++i)
            if (this.normalMapKeys.get(i) === key) return this.normalMaps.get(i);
        let bumpMap: IndexedColorImageUncompressed | null;
        try {
            const resolved = this.resolveResource(path);
            bumpMap = ImagePersistence.importIndexedColor(ReaderMitScene.readFile(resolved), resolved);
        } catch {
            bumpMap = null;
        }
        if (bumpMap !== null && (bumpMap.getXSize() <= 0 || bumpMap.getYSize() <= 0)) {
            bumpMap = null;
        }
        if (bumpMap === null) return null;
        const normalMap = new NormalMap();
        normalMap.importBumpMap(bumpMap, scale);
        scene.addNormalMap(normalMap);
        this.normalMapKeys.add(key);
        this.normalMaps.add(normalMap);
        return normalMap;
    }

    /** Applies the current rotation about the body's origin. */
    private applyRotation(body: SimpleBody): void {
        if (!this.hasRotation()) return;
        const rotation = new Matrix4x4d().eulerAnglesRotation(this.yaw, this.pitch, this.roll);
        body.setRotation(rotation);
        body.setRotationInverse(rotation.inverse());
    }

    /** Applies the current rotation, texture and normal map. */
    private dressBody(body: SimpleBody): void {
        this.applyRotation(body);
        if (this.currentTexture !== null) body.setTextureReference(this.currentTexture);
        if (this.currentNormalMap !== null) body.setNormalMapReference(this.currentNormalMap);
    }

    /**
    Moves the bodies of an OBJ file into the scene, placed by the current
    rotation, the translation and a uniform scale; false when unreadable.
    */
    private importObj(
        path: string,
        translation: Vector3Dd,
        uniformScale: number,
        fallbackMaterial: SimpleMaterial,
        scene: SimpleScene,
    ): boolean {
        const resolved = this.resolveResource(path);
        if (!ReaderMitScene.canOpen(resolved)) return false;
        const imported = new SimpleScene();
        EnvironmentPersistence.importEnvironment(new File(resolved), imported);
        const bodies = imported.getSimpleBodies();
        if (bodies.isEmpty()) return false;
        const sceneRotation = new Matrix4x4d().eulerAnglesRotation(this.yaw, this.pitch, this.roll);
        for (let i = 0; i < bodies.size(); ++i) {
            const body = bodies.get(i);
            if (body.getMaterial() === null) body.setMaterial(new SimpleMaterial(fallbackMaterial));
            const rotation = sceneRotation.multiply(body.getRotation());
            const scale = body.getScale().multiply(uniformScale);
            const position = sceneRotation.multiply(body.getPosition().multiply(uniformScale)).add(translation);
            body.setRotation(rotation);
            body.setRotationInverse(rotation.inverse());
            body.setScale(scale);
            body.setPosition(position);
            scene.addBody(body);
        }
        // The bodies now belong to the target scene.
        bodies.clear();
        return true;
    }

    private static trimLine(input: string): string {
        let a = 0;
        while (
            a < input.length &&
            (input.charAt(a) === " " ||
                input.charAt(a) === "\t" ||
                input.charAt(a) === "\r" ||
                input.charAt(a) === "\n")
        )
            a++;
        let b = input.length;
        while (
            b > a &&
            (input.charAt(b - 1) === " " ||
                input.charAt(b - 1) === "\t" ||
                input.charAt(b - 1) === "\r" ||
                input.charAt(b - 1) === "\n")
        )
            b--;
        return input.substring(a, b);
    }

    /** Strips the quotes around a file name. */
    private static unquote(text: string): string {
        let value = text;
        if (value.length >= 2 && value.charAt(0) === '"' && value.charAt(value.length - 1) === '"')
            value = value.substring(1, value.length - 1);
        return value;
    }

    private static fail(message: string): never {
        throw new VSDKFatalException(message);
    }

    /**
    The successive results of C `fgets(buffer, 1024, file)`: lines up to and
    including their newline, split every 1023 bytes.
    */
    private static fgetsLines(is: InputStream): ArrayList<string> {
        const bytes = is.readAllBytes();
        // Bytes map one to one to characters, as the cpp reader sees them.
        let text = "";
        const chunk = 8192;
        for (let i = 0; i < bytes.length; i += chunk)
            text += String.fromCharCode(...bytes.subarray(i, Math.min(bytes.length, i + chunk)));
        const lines = new ArrayList<string>();
        let start = 0;
        while (start < text.length) {
            let end = start;
            while (end < text.length && end - start < 1023) {
                end++;
                if (text.charAt(end - 1) === "\n") break;
            }
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines;
    }

    /** Reads the scene file; relative resources are resolved from its directory. */
    public importEnvironment(fileName: string, outScene: SimpleScene | null): void;
    /**
    TypeScript and Java only: reads a scene from a stream. Without a file name,
    relative resources are resolved from the working directory.
    */
    public importEnvironment(is: InputStream, outScene: SimpleScene | null): void;
    public importEnvironment(source: string | InputStream, outScene: SimpleScene | null): void {
        if (outScene === null) {
            Logger.reportMessage(this, Logger.ERROR, "importEnvironment", "outScene is null");
            throw new VSDKFatalException("ReaderMitScene::importEnvironment: outScene is null");
        }
        if (typeof source !== "string") {
            const lines = ReaderMitScene.fgetsLines(source);
            source.close();
            this.importLines("", lines, outScene);
            return;
        }
        let lines: ArrayList<string>;
        try {
            const is = new FileInputStream(source);
            try {
                lines = ReaderMitScene.fgetsLines(is);
            } finally {
                is.close();
            }
        } catch {
            Logger.reportMessage(this, Logger.ERROR, "importEnvironment", "could not open file");
            throw new VSDKFatalException("ReaderMitScene::importEnvironment: could not open file");
        }
        this.importLines(source, lines, outScene);
    }

    private importLines(fileName: string, lines: ArrayList<string>, outScene: SimpleScene): void {
        const fail: (message: string) => never = ReaderMitScene.fail;
        const sscanf = _CScanf.sscanf;
        outScene.clearOwnedElements();
        this.resetState(fileName);

        const camera = new Camera();
        const background = new SimpleBackground();
        outScene.addCamera(camera);
        outScene.addBackground(background);
        outScene.setActiveCameraIndex(0);
        outScene.setActiveBackgroundIndex(0);

        let eye = new Vector3Dd(0, 0, 10);
        let up = new Vector3Dd(0, 1, 0);
        let lookat = camera.getFocusedPosition();
        let hasEye = false;
        let hasUp = false;
        let hasLookat = false;
        let hasViewport = false;
        let viewportXSize = 320;
        let viewportYSize = 240;
        let importedHorizontalFov = 30.0;

        let currentMaterial = new SimpleMaterial();
        const triangleGroup = new ReaderMitTriangleGroup();
        let inTriangles = false;
        const quadGroup = new ReaderMitQuadGroup();
        let inQuads = false;
        // Open CSG body and its node stack; primitives inside become its leaves.
        let csgBody: CsgBody | null = null;
        const csgNodes = new ArrayList<number>();
        // Open polynomial or blob block.
        let polynomial: PolynomialSurface | null = null;
        let blob: BlobSolid | null = null;
        // Open patch block: its subdivision settings and control points so far.
        let inPatch = false;
        let patchSubdivision = BezierPatchSubdivision.UniformGrid;
        let patchUSteps = 0;
        let patchVSteps = 0;
        let patchFlatness = 0;
        let patchPointCount = 0;
        const patchPoints: Vector3Dd[][] = [];
        for (let i = 0; i < 4; i++) {
            patchPoints.push([]);
            for (let j = 0; j < 4; j++) patchPoints[i]!.push(new Vector3Dd());
        }
        // Pending placement of the next primitive or outermost csg block.
        const pending = new _PendingPlacement();

        for (let lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            const lineBuf = lines.get(lineIndex);
            // Remove comment
            const sharp = lineBuf.indexOf("#");
            let javaLine = sharp >= 0 ? lineBuf.substring(0, sharp) : lineBuf;
            javaLine = ReaderMitScene.trimLine(javaLine);
            if (javaLine.length === 0) {
                continue;
            }

            const cmdStr = [""];
            sscanf(javaLine, "%255s", cmdStr);
            const cmd = cmdStr[0]!;

            if (polynomial !== null && cmd !== "term" && cmd !== "end")
                fail("ReaderMitScene: poly blocks only hold term lines");
            if (blob !== null && cmd !== "component" && cmd !== "end")
                fail("ReaderMitScene: blob blocks only hold component lines");
            if (inPatch && cmd !== "control" && cmd !== "end")
                fail("ReaderMitScene: patch blocks only hold control lines");
            if (inTriangles && cmd !== "v" && cmd !== "f" && cmd !== "surface" && cmd !== "end")
                fail("ReaderMitScene: triangles blocks only hold v, f and surface lines");
            if (inQuads && cmd !== "v" && cmd !== "q" && cmd !== "surface" && cmd !== "end")
                fail("ReaderMitScene: quads blocks only hold v, q and surface lines");
            if (cmd === "transform") {
                const m: number[][] = [];
                for (let i = 0; i < 12; i++) m.push([0]);
                if (
                    sscanf(
                        javaLine,
                        "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        m[0]!,
                        m[1]!,
                        m[2]!,
                        m[3]!,
                        m[4]!,
                        m[5]!,
                        m[6]!,
                        m[7]!,
                        m[8]!,
                        m[9]!,
                        m[10]!,
                        m[11]!,
                    ) !== 12
                )
                    fail("ReaderMitScene: transform requires twelve numbers");
                for (let i = 0; i < 12; ++i)
                    if (!Double.isFinite(m[i]![0]!)) fail("ReaderMitScene: transform requires finite numbers");
                if (pending.hasPlacement) fail("ReaderMitScene: transform must be followed by a primitive");
                const v = (i: number): number => m[i]![0]!;
                const rows = [
                    [v(0), v(1), v(2), v(3)],
                    [v(4), v(5), v(6), v(7)],
                    [v(8), v(9), v(10), v(11)],
                    [0, 0, 0, 1],
                ];
                pending.placement = new Matrix4x4d(rows);
                pending.hasPlacement = true;
            } else if (cmd === "poly") {
                const order = [0];
                const option = [""];
                const fields = sscanf(javaLine, "%*s %d %255s", order, option);
                if (fields < 1 || !PolynomialSurface.isValidOrder(order[0]!) || (fields === 2 && option[0] !== "sturm"))
                    fail("ReaderMitScene: poly requires an order from 2 to 7 and optionally sturm");
                polynomial = new PolynomialSurface(order[0]!, fields === 2);
            } else if (cmd === "term" && polynomial !== null) {
                const coefficient = [0];
                const x = [0];
                const y = [0];
                const z = [0];
                if (
                    sscanf(javaLine, "%*s %lf %d %d %d", coefficient, x, y, z) !== 4 ||
                    !Double.isFinite(coefficient[0]!)
                )
                    fail("ReaderMitScene: term requires a coefficient and three powers");
                const index = PolynomialSurface.getTermIndex(polynomial.getOrder(), x[0]!, y[0]!, z[0]!);
                if (index < 0) fail("ReaderMitScene: term powers exceed the polynomial order");
                polynomial.getCoefficients()[index]! += coefficient[0]!;
            } else if (cmd === "end" && polynomial !== null) {
                const body = new SimpleBody();
                body.setGeometry(polynomial);
                polynomial = null;
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "blob") {
                const threshold = [0];
                const option = [""];
                const fields = sscanf(javaLine, "%*s %lf %255s", threshold, option);
                if (fields < 1 || !Double.isFinite(threshold[0]!) || (fields === 2 && option[0] !== "sturm"))
                    fail("ReaderMitScene: blob requires a threshold and optionally sturm");
                blob = new BlobSolid(threshold[0]!, fields === 2);
            } else if (cmd === "component" && blob !== null) {
                const strength = [0];
                const radius = [0];
                const x = [0];
                const y = [0];
                const z = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf %lf %lf", strength, radius, x, y, z) !== 5)
                    fail("ReaderMitScene: component requires strength, radius and center");
                const component = new BlobComponent(new Vector3Dd(x[0]!, y[0]!, z[0]!), radius[0]!, strength[0]!);
                if (component.isDegenerate()) fail("ReaderMitScene: degenerate blob component");
                blob.addComponent(component);
            } else if (cmd === "end" && blob !== null) {
                if (blob.getComponentCount() === 0) fail("ReaderMitScene: blob requires a component");
                const body = new SimpleBody();
                body.setGeometry(blob);
                blob = null;
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "patch") {
                if (csgBody !== null) fail("ReaderMitScene: patches are open surfaces and cannot be csg operands");
                const method = [""];
                const option = [""];
                const uSteps = [0];
                const vSteps = [0];
                const flatnessValue = [0.1];
                const fields = sscanf(
                    javaLine,
                    "%*s %255s %d %d %255s %lf",
                    method,
                    uSteps,
                    vSteps,
                    option,
                    flatnessValue,
                );
                const name = method[0]!;
                if (name === "uniform") patchSubdivision = BezierPatchSubdivision.UniformGrid;
                else if (name === "grid") patchSubdivision = BezierPatchSubdivision.PrecomputedGrid;
                else if (name === "adaptive") patchSubdivision = BezierPatchSubdivision.Adaptive;
                else if (name === "tree") patchSubdivision = BezierPatchSubdivision.AdaptiveTree;
                else if (name === "smooth") patchSubdivision = BezierPatchSubdivision.SmoothGrid;
                else fail("ReaderMitScene: patch requires uniform, grid, adaptive, tree or smooth");
                if (
                    !(
                        fields === 3 ||
                        (fields === 5 &&
                            option[0] === "flatness" &&
                            Double.isFinite(flatnessValue[0]!) &&
                            flatnessValue[0]! >= 0)
                    ) ||
                    uSteps[0]! < 1 ||
                    vSteps[0]! < 1 ||
                    uSteps[0]! > 64 ||
                    vSteps[0]! > 64
                )
                    fail("ReaderMitScene: patch requires steps from 1 to 64 and optionally flatness <value>");
                inPatch = true;
                patchUSteps = uSteps[0]!;
                patchVSteps = vSteps[0]!;
                patchFlatness = flatnessValue[0]!;
                patchPointCount = 0;
            } else if (cmd === "control" && inPatch) {
                const x = [0];
                const y = [0];
                const z = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) !== 3 ||
                    !Double.isFinite(x[0]!) ||
                    !Double.isFinite(y[0]!) ||
                    !Double.isFinite(z[0]!)
                )
                    fail("ReaderMitScene: control requires three finite coordinates");
                if (patchPointCount >= 16) fail("ReaderMitScene: patch takes 16 control points");
                patchPoints[Math.trunc(patchPointCount / 4)]![patchPointCount % 4] = new Vector3Dd(x[0]!, y[0]!, z[0]!);
                patchPointCount++;
            } else if (cmd === "end" && inPatch) {
                if (patchPointCount !== 16) fail("ReaderMitScene: patch takes 16 control points");
                inPatch = false;
                const body = new SimpleBody();
                body.setGeometry(
                    new SubdividedBezierPatch(patchSubdivision, patchUSteps, patchVSteps, patchFlatness, patchPoints),
                );
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "heightfield") {
                const path = [""];
                const option = [""];
                const water = [0];
                const fields = sscanf(javaLine, "%*s %511s %255s %lf", path, option, water);
                if (fields !== 1 && !(fields === 3 && option[0] === "water" && Double.isFinite(water[0]!)))
                    fail("ReaderMitScene: heightfield requires a file and optionally water <level>");
                // Relative files are resolved from the scene file's directory.
                let resolved = path[0]!;
                const slash = fileName.lastIndexOf("/");
                if (!path[0]!.startsWith("/") && slash >= 0) resolved = fileName.substring(0, slash + 1) + resolved;
                let heightField: HeightFieldSolid | null = null;
                try {
                    heightField = ReaderAltimetry.importHeightField(resolved);
                } catch (e) {
                    if (!(e instanceof VSDKFatalException)) throw e;
                    fail("ReaderMitScene: could not read the heightfield altimetry file");
                }
                if (fields === 3) {
                    const minimum = heightField.getMinimum().withY(water[0]!);
                    const maximum = heightField.getMaximum();
                    if (!(water[0]! < maximum.y())) {
                        fail("ReaderMitScene: heightfield water level must lie below its highest point");
                    }
                    heightField.setBounds(minimum, maximum);
                    heightField.updateBlocks();
                }
                const body = new SimpleBody();
                body.setGeometry(heightField);
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "triangles") {
                if (csgBody !== null) fail("ReaderMitScene: triangles cannot be csg operands");
                if (pending.hasPlacement) fail("ReaderMitScene: triangles cannot be transformed");
                if (inTriangles || inQuads) fail("ReaderMitScene: nested mesh block");
                triangleGroup.reset();
                inTriangles = true;
            } else if (cmd === "v" && inTriangles) {
                const x = [0];
                const y = [0];
                const z = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) !== 3 ||
                    !triangleGroup.addVertex(new Vector3Dd(x[0]!, y[0]!, z[0]!))
                ) {
                    throw new VSDKFatalException("ReaderMitScene: invalid triangle vertex");
                }
            } else if (cmd === "f" && inTriangles) {
                if (!triangleGroup.addFace(javaLine.substring(1))) {
                    throw new VSDKFatalException("ReaderMitScene: invalid triangle face");
                }
            } else if (cmd === "end" && inTriangles) {
                const mesh = triangleGroup.flush(outScene, currentMaterial);
                if (mesh !== null) this.applyRotation(mesh);
                triangleGroup.reset();
                inTriangles = false;
            } else if (cmd === "csg") {
                const operationStr = [""];
                const evaluatorStr = [""];
                const fields = sscanf(javaLine, "%*s %255s %255s", operationStr, evaluatorStr);
                const operationName = operationStr[0]!;
                const evaluatorName = evaluatorStr[0]!;
                let operation = CsgOperation.UNION;
                if (operationName === "union") operation = CsgOperation.UNION;
                else if (operationName === "intersection") operation = CsgOperation.INTERSECTION;
                else if (operationName === "difference") operation = CsgOperation.DIFFERENCE;
                else fail("ReaderMitScene: csg requires union, intersection or difference");
                if (csgBody === null) {
                    let evaluator = CsgBodyEvaluator.ROTH;
                    if (fields === 2 && evaluatorName === "pov") evaluator = CsgBodyEvaluator.CROSSING_MEMBERSHIP;
                    else if (fields === 2 && evaluatorName !== "roth")
                        fail("ReaderMitScene: csg evaluator must be roth or pov");
                    csgBody = new CsgBody(operation, evaluator);
                    csgBody.setMaterial(new SimpleMaterial(currentMaterial));
                    if (!ReaderMitScene.place(csgBody, pending))
                        fail("ReaderMitScene: transform is not an invertible affine map");
                    csgNodes.add(0);
                } else {
                    if (fields === 2) fail("ReaderMitScene: only the outermost csg selects an evaluator");
                    if (pending.hasPlacement) fail("ReaderMitScene: nested csg blocks cannot be transformed");
                    const node = csgBody.addNode(csgNodes.get(csgNodes.size() - 1), operation);
                    if (node < 0) fail("ReaderMitScene: invalid csg node");
                    csgNodes.add(node);
                }
            } else if (cmd === "end" && csgBody !== null) {
                csgNodes.remove(csgNodes.size() - 1);
                if (csgNodes.size() === 0) {
                    outScene.addBody(csgBody);
                    csgBody = null;
                }
            } else if (cmd === "viewport") {
                const w = [0];
                const h = [0];
                if (sscanf(javaLine, "%*s %d %d", w, h) === 2) {
                    viewportXSize = w[0]!;
                    viewportYSize = h[0]!;
                    hasViewport = true;
                }
            } else if (cmd === "eye") {
                const x = [0];
                const y = [0];
                const z = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) === 3) {
                    eye = new Vector3Dd(x[0]!, y[0]!, z[0]!);
                    hasEye = true;
                }
            } else if (cmd === "up") {
                const x = [0];
                const y = [0];
                const z = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) === 3) {
                    up = new Vector3Dd(x[0]!, y[0]!, z[0]!);
                    hasUp = true;
                }
            } else if (cmd === "lookat") {
                const x = [0];
                const y = [0];
                const z = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) === 3) {
                    lookat = new Vector3Dd(x[0]!, y[0]!, z[0]!);
                    hasLookat = true;
                }
            } else if (cmd === "fov") {
                const fov = [0];
                if (sscanf(javaLine, "%*s %lf", fov) === 1) {
                    importedHorizontalFov = fov[0]!;
                }
            } else if (cmd === "background") {
                const r = [0];
                const g = [0];
                const b = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf", r, g, b) === 3) {
                    background.setColor(r[0]!, g[0]!, b[0]!);
                    outScene.setActiveBackgroundIndex(0);
                }
            } else if (cmd === "surface") {
                if (inTriangles) {
                    const mesh = triangleGroup.flush(outScene, currentMaterial);
                    if (mesh !== null) this.applyRotation(mesh);
                }
                if (inQuads) {
                    const mesh = quadGroup.flush(outScene, currentMaterial);
                    if (mesh !== null) this.applyRotation(mesh);
                }
                const r = [0];
                const g = [0];
                const b = [0];
                const ka = [0];
                const kd = [0];
                const ks = [0];
                const ns = [0];
                const kr = [0];
                const kt = [0];
                const index = [0];
                if (
                    sscanf(
                        javaLine,
                        "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        r,
                        g,
                        b,
                        ka,
                        kd,
                        ks,
                        ns,
                        kr,
                        kt,
                        index,
                    ) === 10
                ) {
                    currentMaterial = new SimpleMaterial(
                        "surface",
                        new ColorRgb(r[0]! * ka[0]!, g[0]! * ka[0]!, b[0]! * ka[0]!),
                        new ColorRgb(r[0]! * kd[0]!, g[0]! * kd[0]!, b[0]! * kd[0]!),
                        new ColorRgb(ks[0]!, ks[0]!, ks[0]!),
                        new ColorRgb(0.0, 0.0, 0.0),
                        new ColorRgb(0.0, 0.0, 0.0),
                        true,
                        kr[0]!,
                        kt[0]!,
                        index[0]!,
                        1.0,
                        ns[0]!,
                    );
                }
            } else if (cmd === "box") {
                const xmin = [0];
                const ymin = [0];
                const zmin = [0];
                const xmax = [0];
                const ymax = [0];
                const zmax = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf", xmin, ymin, zmin, xmax, ymax, zmax) !== 6 ||
                    !Double.isFinite(xmin[0]!) ||
                    !Double.isFinite(ymin[0]!) ||
                    !Double.isFinite(zmin[0]!) ||
                    !Double.isFinite(xmax[0]!) ||
                    !Double.isFinite(ymax[0]!) ||
                    !Double.isFinite(zmax[0]!) ||
                    xmin[0]! >= xmax[0]! ||
                    ymin[0]! >= ymax[0]! ||
                    zmin[0]! >= zmax[0]!
                ) {
                    throw new VSDKFatalException("ReaderMitScene: box requires ordered minimum and maximum corners");
                }
                const b = new SimpleBody();
                b.setGeometry(
                    new Box(new Vector3Dd(xmin[0]!, ymin[0]!, zmin[0]!), new Vector3Dd(xmax[0]!, ymax[0]!, zmax[0]!)),
                );
                b.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(b, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "halfspace") {
                const a = [0];
                const b = [0];
                const c = [0];
                const d = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf %lf", a, b, c, d) !== 4 ||
                    !Double.isFinite(a[0]!) ||
                    !Double.isFinite(b[0]!) ||
                    !Double.isFinite(c[0]!) ||
                    !Double.isFinite(d[0]!) ||
                    (a[0] === 0 && b[0] === 0 && c[0] === 0)
                ) {
                    throw new VSDKFatalException("ReaderMitScene: halfspace requires a nonzero normal and constant");
                }
                const body = new SimpleBody();
                body.setGeometry(new PlanarHalfSpace(a[0]!, b[0]!, c[0]!, d[0]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "quadric") {
                const xx = [0];
                const yy = [0];
                const zz = [0];
                const xy = [0];
                const xz = [0];
                const yz = [0];
                const x = [0];
                const y = [0];
                const z = [0];
                const c = [0];
                if (
                    sscanf(
                        javaLine,
                        "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        xx,
                        yy,
                        zz,
                        xy,
                        xz,
                        yz,
                        x,
                        y,
                        z,
                        c,
                    ) !== 10
                ) {
                    throw new VSDKFatalException("ReaderMitScene: quadric requires ten coefficients");
                }
                const b = new SimpleBody();
                b.setGeometry(
                    new QuadricSurface(
                        new Vector3Dd(xx[0]!, yy[0]!, zz[0]!),
                        new Vector3Dd(xy[0]!, xz[0]!, yz[0]!),
                        new Vector3Dd(x[0]!, y[0]!, z[0]!),
                        c[0]!,
                    ),
                );
                b.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(b, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "sphere") {
                const x = [0];
                const y = [0];
                const z = [0];
                const r = [0];
                if (sscanf(javaLine, "%*s %lf %lf %lf %lf", x, y, z, r) === 4) {
                    const b = new SimpleBody();
                    b.setGeometry(new Sphere(r[0]!));
                    b.setPosition(new Vector3Dd(x[0]!, y[0]!, z[0]!));
                    b.setMaterial(new SimpleMaterial(currentMaterial));
                    this.attach(b, pending, csgBody, csgNodes, outScene);
                } else fail("ReaderMitScene: sphere requires a center and a radius");
            } else if (cmd === "light") {
                const r = [0];
                const g = [0];
                const b = [0];
                const typeStr = [""];
                if (sscanf(javaLine, "%*s %lf %lf %lf %255s", r, g, b, typeStr) === 4) {
                    const type = typeStr[0]!;
                    if (type === "ambient") {
                        outScene.addLight(new AmbientLight(new ColorRgb(r[0]!, g[0]!, b[0]!)));
                    } else if (type === "point") {
                        const x = [0];
                        const y = [0];
                        const z = [0];
                        if (sscanf(javaLine, "%*s %*f %*f %*f %*s %lf %lf %lf", x, y, z) === 3) {
                            outScene.addLight(
                                new PointLight(new Vector3Dd(x[0]!, y[0]!, z[0]!), new ColorRgb(r[0]!, g[0]!, b[0]!)),
                            );
                        }
                    } else if (type === "directional") {
                        const x = [0];
                        const y = [0];
                        const z = [0];
                        if (sscanf(javaLine, "%*s %*f %*f %*f %*s %lf %lf %lf", x, y, z) === 3) {
                            outScene.addLight(
                                new DirectionalLight(
                                    new Vector3Dd(x[0]!, y[0]!, z[0]!),
                                    new ColorRgb(r[0]!, g[0]!, b[0]!),
                                ),
                            );
                        } else fail("ReaderMitScene: directional light requires a direction");
                    } else fail("ReaderMitScene: light type must be ambient, point or directional");
                } else fail("ReaderMitScene: light requires a color and a type");
            } else if (cmd === "cube") {
                const x = [0];
                const y = [0];
                const z = [0];
                const size = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf %lf", x, y, z, size) !== 4 ||
                    !Double.isFinite(size[0]!) ||
                    size[0]! <= 0
                )
                    fail("ReaderMitScene: cube requires a center and a positive size");
                const body = new SimpleBody();
                body.setGeometry(new Box(size[0]!, size[0]!, size[0]!));
                body.setPosition(new Vector3Dd(x[0]!, y[0]!, z[0]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "cylinder") {
                const x = [0];
                const y = [0];
                const z = [0];
                const bottomRadius = [0];
                const topRadius = [0];
                const height = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf", x, y, z, bottomRadius, topRadius, height) !== 6 ||
                    !(bottomRadius[0]! >= 0) ||
                    !(topRadius[0]! >= 0) ||
                    !(height[0]! > 0) ||
                    (bottomRadius[0] === 0 && topRadius[0] === 0)
                )
                    fail("ReaderMitScene: cylinder requires a base center, two radii and a positive height");
                if (csgBody !== null) fail("ReaderMitScene: cylinder cannot be a csg operand");
                const body = new SimpleBody();
                body.setGeometry(new Cone(bottomRadius[0]!, topRadius[0]!, height[0]!));
                body.setPosition(new Vector3Dd(x[0]!, y[0]!, z[0]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "torus") {
                const x = [0];
                const y = [0];
                const z = [0];
                const majorRadius = [0];
                const minorRadius = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf %lf %lf", x, y, z, majorRadius, minorRadius) !== 5 ||
                    !(majorRadius[0]! > 0) ||
                    !(minorRadius[0]! > 0)
                )
                    fail("ReaderMitScene: torus requires a center and two positive radii");
                if (csgBody !== null) fail("ReaderMitScene: torus cannot be a csg operand");
                const body = new SimpleBody();
                body.setGeometry(new Torus(majorRadius[0]!, minorRadius[0]!));
                body.setPosition(new Vector3Dd(x[0]!, y[0]!, z[0]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "prism") {
                // prism x y z height n x1 y1 ... xn yn: a simple polygon of n
                // vertices on the plane z = 0 of the body, placed at (x, y, z),
                // swept height units along +Z. It is a parametric solid, so it
                // can be a csg operand.
                let cursor = 0;
                while (cursor < javaLine.length && javaLine.charAt(cursor) !== " " && javaLine.charAt(cursor) !== "\t")
                    cursor++;
                const values = [0, 0, 0, 0, 0];
                let ok = true;
                for (let i = 0; i < 5 && ok; i++) {
                    const parsed = [0];
                    const next = _CScanf.strtod(javaLine, cursor, parsed);
                    values[i] = parsed[0]!;
                    ok = next !== cursor && Double.isFinite(values[i]!);
                    cursor = next;
                }
                // C++ `(int)values[4]` of a value in [3, 100000]
                const n = ok && values[4]! >= 3 && values[4]! <= 100000 ? Math.trunc(values[4]!) : 0;
                ok = ok && n >= 3 && values[4] === n && values[3]! > 0;
                const polygon = new ArrayList<Vector2Dd>();
                for (let i = 0; i < 2 * n && ok; i += 2) {
                    const parsed = [0];
                    let next = _CScanf.strtod(javaLine, cursor, parsed);
                    const px = parsed[0]!;
                    ok = next !== cursor && Double.isFinite(px);
                    cursor = next;
                    if (!ok) break;
                    next = _CScanf.strtod(javaLine, cursor, parsed);
                    const py = parsed[0]!;
                    ok = next !== cursor && Double.isFinite(py);
                    cursor = next;
                    polygon.add(new Vector2Dd(px, py));
                }
                while (ok && (javaLine.charAt(cursor) === " " || javaLine.charAt(cursor) === "\t")) cursor++;
                ok = ok && cursor >= javaLine.length;
                if (!ok)
                    fail(
                        "ReaderMitScene: prism requires a base position, a positive height, a vertex count of at least 3 and that many x y pairs",
                    );
                const prism = new Prism(polygon, values[3]!);
                if (!prism.hasValidParameters()) {
                    fail("ReaderMitScene: prism base must be a simple polygon with non-zero area");
                }
                const body = new SimpleBody();
                body.setGeometry(prism);
                body.setPosition(new Vector3Dd(values[0]!, values[1]!, values[2]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "polybox" || cmd === "appel") {
                if (csgBody !== null) fail("ReaderMitScene: polyhedral solids cannot be csg operands");
                const x = [0];
                const y = [0];
                const z = [0];
                const scale = [1.0];
                let solid: PolyhedralBoundedSolid | null = null;
                if (cmd === "polybox") {
                    const sx = [0];
                    const sy = [0];
                    const sz = [0];
                    if (
                        sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf", x, y, z, sx, sy, sz) !== 6 ||
                        !(sx[0]! > 0) ||
                        !(sy[0]! > 0) ||
                        !(sz[0]! > 0)
                    )
                        fail("ReaderMitScene: polybox requires a center and three positive sizes");
                    const box = new Box(sx[0]!, sy[0]!, sz[0]!);
                    solid = box.exportToPolyhedralBoundedSolid();
                } else {
                    const id = [0];
                    const fields = sscanf(javaLine, "%*s %d %lf %lf %lf %lf", id, x, y, z, scale);
                    if (fields < 4 || (fields === 5 && !(scale[0]! > 0)) || id[0]! < 1 || id[0]! > 3)
                        fail(
                            "ReaderMitScene: appel requires an object 1 to 3, a center and optionally a positive scale",
                        );
                    if (id[0] === 1) solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_1();
                    else if (id[0] === 2) solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_2();
                    else solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
                    // The APPEL objects are modelled in [0,1]^3; center them on the origin.
                    PolyhedralBoundedSolidModeler.applyTransformation(
                        solid,
                        new Matrix4x4d().translation(-0.5, -0.5, -0.5),
                    );
                }
                if (solid === null) fail("ReaderMitScene: could not build the polyhedral solid");
                const body = new SimpleBody();
                body.setGeometry(solid);
                body.setPosition(new Vector3Dd(x[0]!, y[0]!, z[0]!));
                body.setScale(new Vector3Dd(scale[0]!, scale[0]!, scale[0]!));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                this.attach(body, pending, csgBody, csgNodes, outScene);
            } else if (cmd === "obj") {
                const path = [""];
                const x = [0];
                const y = [0];
                const z = [0];
                const scale = [1.0];
                const fields = sscanf(javaLine, "%*s %511s %lf %lf %lf %lf", path, x, y, z, scale);
                if (fields < 4 || (fields === 5 && !(scale[0]! > 0)))
                    fail("ReaderMitScene: obj requires a file, a position and optionally a positive scale");
                if (csgBody !== null || pending.hasPlacement)
                    fail("ReaderMitScene: obj models cannot be csg operands or transformed");
                if (
                    !this.importObj(
                        ReaderMitScene.unquote(path[0]!),
                        new Vector3Dd(x[0]!, y[0]!, z[0]!),
                        scale[0]!,
                        currentMaterial,
                        outScene,
                    )
                )
                    fail("ReaderMitScene: could not read the obj file");
            } else if (cmd === "quads") {
                if (csgBody !== null) fail("ReaderMitScene: quads cannot be csg operands");
                if (pending.hasPlacement) fail("ReaderMitScene: quads cannot be transformed");
                if (inQuads || inTriangles) fail("ReaderMitScene: nested mesh block");
                quadGroup.reset();
                inQuads = true;
            } else if (cmd === "v" && inQuads) {
                const x = [0];
                const y = [0];
                const z = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) !== 3 ||
                    !quadGroup.addVertex(new Vector3Dd(x[0]!, y[0]!, z[0]!))
                )
                    fail("ReaderMitScene: invalid quad vertex");
            } else if (cmd === "q" && inQuads) {
                if (!quadGroup.addQuad(javaLine.substring(1))) fail("ReaderMitScene: invalid quad face");
            } else if (cmd === "end" && inQuads) {
                const mesh = quadGroup.flush(outScene, currentMaterial);
                if (mesh !== null) this.applyRotation(mesh);
                quadGroup.reset();
                inQuads = false;
            } else if (cmd === "rotation") {
                const a = [0];
                const b = [0];
                const c = [0];
                if (
                    sscanf(javaLine, "%*s %lf %lf %lf", a, b, c) !== 3 ||
                    !Double.isFinite(a[0]!) ||
                    !Double.isFinite(b[0]!) ||
                    !Double.isFinite(c[0]!)
                )
                    fail("ReaderMitScene: rotation requires three finite angles");
                this.yaw = a[0]!;
                this.pitch = b[0]!;
                this.roll = c[0]!;
            } else if (cmd === "texture") {
                const path = [""];
                if (sscanf(javaLine, "%*s %511s", path) !== 1) fail("ReaderMitScene: texture requires a file");
                this.currentTexture = this.loadTexture(ReaderMitScene.unquote(path[0]!), outScene);
                if (this.currentTexture === null) fail("ReaderMitScene: could not read the texture file");
            } else if (cmd === "notexture") {
                this.currentTexture = null;
            } else if (cmd === "bumpmap") {
                const path = [""];
                const sx = [1];
                const sy = [1];
                const sz = [0.2];
                const fields = sscanf(javaLine, "%*s %511s %lf %lf %lf", path, sx, sy, sz);
                if (fields !== 1 && fields !== 4)
                    fail("ReaderMitScene: bumpmap requires a file and optionally three scale factors");
                this.currentNormalMap = this.loadNormalMap(
                    ReaderMitScene.unquote(path[0]!),
                    new Vector3Dd(sx[0]!, sy[0]!, sz[0]!),
                    outScene,
                );
                if (this.currentNormalMap === null) fail("ReaderMitScene: could not read the bump map file");
            } else if (cmd === "nobumpmap") {
                this.currentNormalMap = null;
            } else if (cmd === "backgroundcubemap") {
                const directoryStr = [""];
                let directory = "../../cubemaps/dorise1";
                if (sscanf(javaLine, "%*s %511s", directoryStr) === 1)
                    directory = ReaderMitScene.unquote(directoryStr[0]!);
                const faces: (RGBAImageUncompressed | null)[] = [null, null, null, null, null, null];
                for (let i = 0; i < 6; ++i) {
                    const name = "/entorno" + i + ".jpg";
                    try {
                        faces[i] = ReaderMitScene.importRGBA(this.resolveResource(directory + name));
                    } catch {
                        faces[i] = null;
                    }
                    if (faces[i] !== null && (faces[i]!.getXSize() <= 0 || faces[i]!.getYSize() <= 0)) {
                        faces[i] = null;
                    }
                    if (faces[i] === null) {
                        fail("ReaderMitScene: could not read the cubemap faces");
                    }
                }
                for (let i = 0; i < 6; ++i) outScene.addImage(faces[i]!);
                outScene.addBackground(
                    new CubemapBackground(camera, faces[0]!, faces[1]!, faces[2]!, faces[3]!, faces[4]!, faces[5]!),
                );
                outScene.setActiveBackgroundIndex(outScene.getBackgrounds().size() - 1);
            } else fail("ReaderMitScene: unsupported command");
        }

        if (polynomial !== null) fail("ReaderMitScene: unterminated poly block");
        if (blob !== null) fail("ReaderMitScene: unterminated blob block");
        if (inPatch) fail("ReaderMitScene: unterminated patch block");
        if (pending.hasPlacement) fail("ReaderMitScene: transform must be followed by a primitive");
        if (csgBody !== null) fail("ReaderMitScene: unterminated csg block");
        if (inTriangles || inQuads) {
            throw new VSDKFatalException("ReaderMitScene: unterminated mesh block");
        }

        if (hasEye) {
            camera.setPosition(eye);
        }
        camera.setUpDirect(up);
        if (hasLookat) {
            camera.setFocusedPositionMaintainingOrthogonality(lookat);
        }
        // cpp keeps `hasUp` for symmetry with the other view settings.
        void hasUp;

        if (viewportXSize > 0 && viewportYSize > 0) {
            const aspect = viewportXSize / viewportYSize;
            const horizontalHalfAngle = (importedHorizontalFov * Math.PI) / 360.0;
            const verticalHalfAngle = Math.atan(Math.tan(horizontalHalfAngle) / aspect);
            const verticalFov = (verticalHalfAngle * 360.0) / Math.PI;
            camera.setFov(verticalFov);
        }
        if (hasViewport) {
            camera.updateViewportResize(viewportXSize, viewportYSize);
        } else {
            camera.updateVectors();
        }
    }

    // cpp lambda `place`.
    private static place(body: SimpleBody, pending: _PendingPlacement): boolean {
        if (!pending.hasPlacement) return true;
        pending.hasPlacement = false;
        const forward = pending.placement.multiply(body.getAffinePlacement());
        return body.setAffinePlacement(forward, forward.inverse()) === AffinePlacementStatus.Valid;
    }

    // cpp lambda `attach`: every primitive takes the current rotation,
    // texture and normal map.
    private attach(
        body: SimpleBody,
        pending: _PendingPlacement,
        csgBody: CsgBody | null,
        csgNodes: ArrayList<number>,
        outScene: SimpleScene,
    ): void {
        this.dressBody(body);
        if (!ReaderMitScene.place(body, pending)) {
            ReaderMitScene.fail("ReaderMitScene: transform is not an invertible affine map");
        }
        if (csgBody === null) {
            outScene.addBody(body);
            return;
        }
        if (!csgBody.addLeaf(csgNodes.get(csgNodes.size() - 1), body))
            ReaderMitScene.fail("ReaderMitScene: invalid csg operand");
    }
}
