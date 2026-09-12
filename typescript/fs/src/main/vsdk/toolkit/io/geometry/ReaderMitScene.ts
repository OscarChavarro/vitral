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
import { BufferedReader } from "@vitral/base/java/io/BufferedReader";
import { InputStream } from "@vitral/base/java/io/InputStream";
import { InputStreamReader } from "@vitral/base/java/io/InputStreamReader";
import { IOException } from "@vitral/base/java/io/IOException";
import type { Reader } from "@vitral/base/java/io/Reader";
import { StreamTokenizer } from "@vitral/base/java/io/StreamTokenizer";
import { ArrayList } from "@vitral/base/java/util/ArrayList";
import { HashMap } from "@vitral/base/java/util/HashMap";
import { Integer } from "@vitral/base/java/lang/Integer";
import { Math as JavaMath } from "@vitral/base/java/lang/Math";

// VSDK classes
import { Triangle } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Triangle";
import { Vertex } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Vertex";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { Matrix4x4d } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Matrix4x4d";
import { ColorRgb } from "@vitral/base/vsdk/toolkit/common/color/ColorRgb";
import { Camera } from "@vitral/base/vsdk/toolkit/environment/camera/Camera";
import { AmbientLight } from "@vitral/base/vsdk/toolkit/environment/light/AmbientLight";
import { DirectionalLight } from "@vitral/base/vsdk/toolkit/environment/light/DirectionalLight";
import { PointLight } from "@vitral/base/vsdk/toolkit/environment/light/PointLight";
import { SimpleMaterial } from "@vitral/base/vsdk/toolkit/environment/material/SimpleMaterial";
import type { Background } from "@vitral/base/vsdk/toolkit/environment/background/Background";
import { SimpleBackground } from "@vitral/base/vsdk/toolkit/environment/background/SimpleBackground";
import { Sphere } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Sphere";
import { Box } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Box";
import { Cone } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Cone";
import { Torus } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Torus";
import type { PolyhedralBoundedSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid";
import { QuadMesh } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/QuadMesh";
import { TriangleMesh } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/TriangleMesh";
import { SimpleBody } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleBody";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import type { NormalMap } from "@vitral/base/vsdk/toolkit/media/NormalMap";
import type { RGBImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBImageUncompressed";
import { PolyhedralBoundedSolidModeler } from "@vitral/base/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler";
import { SimpleTestGeometryLibrary } from "@vitral/base/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary";

import { File } from "../../../../java/io/File.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { EnvironmentPersistence } from "./EnvironmentPersistence.js";

/**
Port of the Java private inner class `ReaderMitScene.ImportContext`.
*/
class _ImportContext {
    public currentCamera: Camera;
    public currentBackground: Background;
    public viewportXSize: number;
    public viewportYSize: number;
    public importedEye: Vector3Dd;
    public importedLookAt: Vector3Dd;
    public importedUp: Vector3Dd;
    public importedHorizontalFov: number;

    public constructor() {
        this.currentCamera = new Camera();
        this.currentBackground = new SimpleBackground();
        (this.currentBackground as SimpleBackground).setColor(0, 0, 0);

        this.viewportXSize = 320;
        this.viewportYSize = 240;
        this.importedEye = new Vector3Dd(0, 0, 10);
        this.importedLookAt = new Vector3Dd(0, 0, 0);
        this.importedUp = new Vector3Dd(0, 1, 0);
        this.importedHorizontalFov = 30;
    }
}

/**
This class implements an scene reader based on the instructional raytracer
from Tomas Lozano-Perez from computer graphics class at MIT on spring 2001,
and from Leonard McMillan 1998. This material was adapted by Oscar Chavarro
for computer graphics classes at Colombia, and later as anoter scene reader
for Vitral.

Unported branches: `texture`, `bumpmap` and `backgroundcubemap` all need to
*import* a raster image file, and image import is not part of the TypeScript
port yet (`@vitral/fs`'s `ImagePersistence` only exports). Those three scene
commands therefore raise instead of silently producing an untextured scene;
the same note applies to `ReaderObj.obtainTextureFromFile`. Wiring an importer
into `loadRgbTexture` / `loadNormalMapFromBump` is all that is left for them.
*/
export class ReaderMitScene extends PersistenceElement {
    public constructor() {
        super();
    }

    private showDebugMessage(m: string): void {
        if (this.showDebugMessages()) {
            console.log(m);
        }
    }

    private showDebugMessages(): boolean {
        return false;
    }

    private readNumber(st: StreamTokenizer): number {
        if (st.nextToken() !== StreamTokenizer.TT_NUMBER) {
            console.error("ERROR: number expected in line " + st.lineno());
            throw new IOException(st.toString());
        }
        return st.nval;
    }

    private readStringToken(st: StreamTokenizer): string {
        const tokenType: number = st.nextToken();
        if (tokenType === StreamTokenizer.TT_WORD || tokenType === '"'.charCodeAt(0)) {
            return st.sval as string;
        }
        console.error("ERROR: string expected in line " + st.lineno());
        throw new IOException(st.toString());
    }

    private readSurfaceDefinition(st: StreamTokenizer): SimpleMaterial {
        const r: number = this.readNumber(st);
        const g: number = this.readNumber(st);
        const b: number = this.readNumber(st);
        const ka: number = this.readNumber(st);
        const kd: number = this.readNumber(st);
        const ks: number = this.readNumber(st);
        const ns: number = this.readNumber(st);
        const kr: number = this.readNumber(st);
        const kt: number = this.readNumber(st);
        const index: number = this.readNumber(st);

        let material: SimpleMaterial = new SimpleMaterial();
        material = material.withAmbient(new ColorRgb(r * ka, g * ka, b * ka));
        material = material.withDiffuse(new ColorRgb(r * kd, g * kd, b * kd));
        material = material.withSpecular(new ColorRgb(ks, ks, ks));
        material = material.withPhongExponent(ns);
        material = material.withReflectionCoefficient(kr);
        material = material.withRefractionCoefficient(kt);
        // `index` is read but unused by the Java original as well.
        void index;
        return material;
    }

    private loadRgbTexture(
        texturePath: string,
        _textureCache: HashMap<string, RGBImageUncompressed>,
    ): RGBImageUncompressed {
        throw new IOException(
            'Scene texture "' +
                texturePath +
                '" can not be loaded: raster image import is not ported to TypeScript yet ' +
                "(@vitral/fs ImagePersistence only exports images).",
        );
    }

    private loadNormalMapFromBump(
        bumpPath: string,
        _bumpScale: Vector3Dd,
        _normalMapCache: HashMap<string, NormalMap>,
    ): NormalMap {
        throw new IOException(
            'Scene bump map "' +
                bumpPath +
                '" can not be loaded: raster image import is not ported to TypeScript yet ' +
                "(@vitral/fs ImagePersistence only exports images).",
        );
    }

    private applyBodyTransform(thing: SimpleBody, yaw: number, pitch: number, roll: number, position: Vector3Dd): void {
        let R: Matrix4x4d = new Matrix4x4d();
        R = R.eulerAnglesRotation(yaw, pitch, roll);
        thing.setRotation(R);
        let Ri: Matrix4x4d = new Matrix4x4d(R);
        Ri = Ri.invert();
        thing.setRotationInverse(Ri);
        thing.setPosition(position);
    }

    private flushTriangleBatch(
        theScene: SimpleScene,
        vertices: ArrayList<Vertex>,
        triangles: ArrayList<Triangle>,
        material: SimpleMaterial | null,
        yaw: number,
        pitch: number,
        roll: number,
    ): void {
        if (triangles.isEmpty()) {
            return;
        }

        const mesh: TriangleMesh = new TriangleMesh();
        mesh.setVertexes(vertices.toArray(), false, false, false, false);
        mesh.setTriangles(triangles.toArray());
        mesh.calculateNormals();

        const thing: SimpleBody = new SimpleBody();
        thing.setGeometry(mesh);
        thing.setMaterial(material);
        this.applyBodyTransform(thing, yaw, pitch, roll, new Vector3Dd());
        theScene.addBody(thing);
    }

    private addImportedObj(
        theScene: SimpleScene,
        objectPath: string,
        fallbackMaterial: SimpleMaterial,
        yaw: number,
        pitch: number,
        roll: number,
        translation: Vector3Dd,
        uniformScale: number,
    ): void {
        const importedScene: SimpleScene = new SimpleScene();
        EnvironmentPersistence.importEnvironment(new File(objectPath), importedScene);

        const sceneRotation: Matrix4x4d = new Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
        const importedBodies: ArrayList<SimpleBody> = importedScene.getSimpleBodies();
        for (let i = 0; i < importedBodies.size(); i++) {
            const importedBody: SimpleBody = importedBodies.get(i);
            const thing: SimpleBody = new SimpleBody();
            thing.setName(importedBody.getName());
            thing.setGeometry(importedBody.getGeometry());
            thing.setMaterial(importedBody.getMaterial() !== null ? importedBody.getMaterial() : fallbackMaterial);
            thing.setTexture(importedBody.getTexture());
            thing.setNormalMap(importedBody.getNormalMap());

            const composedRotation: Matrix4x4d = sceneRotation.multiply(importedBody.getRotation());
            thing.setRotation(composedRotation);
            thing.setRotationInverse(new Matrix4x4d(composedRotation).invert());

            const sourceScale: Vector3Dd = importedBody.getScale();
            thing.setScale(
                new Vector3Dd(
                    sourceScale.x() * uniformScale,
                    sourceScale.y() * uniformScale,
                    sourceScale.z() * uniformScale,
                ),
            );

            const translatedPosition: Vector3Dd = sceneRotation
                .multiply(importedBody.getPosition().multiply(uniformScale))
                .add(translation);
            thing.setPosition(translatedPosition);
            theScene.addBody(thing);
        }
    }

    private flushQuadBatch(
        theScene: SimpleScene,
        vertices: ArrayList<Vertex>,
        quads: ArrayList<number[]>,
        material: SimpleMaterial | null,
        yaw: number,
        pitch: number,
        roll: number,
    ): void {
        if (quads.isEmpty()) {
            return;
        }

        const mesh: QuadMesh = new QuadMesh();
        mesh.setVertexes(vertices.toArray());
        mesh.initQuadArrays(quads.size());
        const quadIndices: Int32Array = mesh.getQuadIndices() as Int32Array;
        for (let i = 0; i < quads.size(); i++) {
            const q: number[] = quads.get(i);
            quadIndices[4 * i] = q[0]!;
            quadIndices[4 * i + 1] = q[1]!;
            quadIndices[4 * i + 2] = q[2]!;
            quadIndices[4 * i + 3] = q[3]!;
        }

        const thing: SimpleBody = new SimpleBody();
        thing.setGeometry(mesh);
        thing.setMaterial(material);
        this.applyBodyTransform(thing, yaw, pitch, roll, new Vector3Dd());
        theScene.addBody(thing);
    }

    private convertMitHorizontalFovToVertical(
        horizontalFov: number,
        viewportXSize: number,
        viewportYSize: number,
    ): number {
        if (viewportXSize <= 0 || viewportYSize <= 0) {
            return horizontalFov;
        }

        const aspect: number = viewportXSize / viewportYSize;
        const horizontalHalfAngle: number = JavaMath.toRadians(horizontalFov / 2.0);
        const verticalHalfAngle: number = Math.atan(Math.tan(horizontalHalfAngle) / aspect);

        return JavaMath.toDegrees(2.0 * verticalHalfAngle);
    }

    private configureCurrentCameraFromMitView(context: _ImportContext): void {
        context.currentCamera.setPosition(context.importedEye);
        context.currentCamera.setUpDirect(context.importedUp);
        context.currentCamera.setFocusedPositionMaintainingOrthogonality(context.importedLookAt);
        context.currentCamera.setFov(
            this.convertMitHorizontalFovToVertical(
                context.importedHorizontalFov,
                context.viewportXSize,
                context.viewportYSize,
            ),
        );
        context.currentCamera.updateViewportResize(context.viewportXSize, context.viewportYSize);
    }

    public importEnvironment(is: InputStream, theScene: SimpleScene): void {
        const context: _ImportContext = new _ImportContext();
        this.configureCurrentCameraFromMitView(context);

        const parsero: Reader = new BufferedReader(new InputStreamReader(is));
        const st: StreamTokenizer = new StreamTokenizer(parsero);
        st.commentChar("#".charCodeAt(0));
        st.quoteChar('"'.charCodeAt(0));
        st.eolIsSignificant(true);
        let fin_de_lectura = false;
        let currentMaterial: SimpleMaterial;
        let currentTrianglesMaterial: SimpleMaterial | null = null;
        let currentQuadsMaterial: SimpleMaterial | null = null;
        let currentTexture: RGBImageUncompressed | null = null;
        let currentNormalMap: NormalMap | null = null;
        const textureCache: HashMap<string, RGBImageUncompressed> = new HashMap<string, RGBImageUncompressed>();
        const normalMapCache: HashMap<string, NormalMap> = new HashMap<string, NormalMap>();

        // SimpleMaterial por defecto...
        /*
        currentMaterial = new SimpleMaterial(0.8f, 0.2f, 0.9f,
                                       0.2f, 0.4f, 0.4f,
                                       10.0f, 0f, 0f, 1f);
        */
        currentMaterial = new SimpleMaterial();
        currentMaterial = currentMaterial.withAmbient(new ColorRgb(0.8 * 0.2, 0.2 * 0.2, 0.9 * 0.2));
        currentMaterial = currentMaterial.withDiffuse(new ColorRgb(0.8 * 0.4, 0.2 * 0.4, 0.9 * 0.4));
        currentMaterial = currentMaterial.withSpecular(new ColorRgb(0.4, 0.4, 0.4));
        currentMaterial = currentMaterial.withReflectionCoefficient(0);
        currentMaterial = currentMaterial.withRefractionCoefficient(0);
        currentMaterial = currentMaterial.withPhongExponent(10);

        let readingTriangles = false;
        let readingQuads = false;
        const triangleVertices: ArrayList<Vertex> = new ArrayList<Vertex>();
        const triangleFaces: ArrayList<Triangle> = new ArrayList<Triangle>();
        const quadVertices: ArrayList<Vertex> = new ArrayList<Vertex>();
        const quadFaces: ArrayList<number[]> = new ArrayList<number[]>();
        let thing: SimpleBody;
        let yaw_actual = 0;
        let pitch_actual = 0;
        let roll_actual = 0;

        while (!fin_de_lectura) {
            const tokenType: number = st.nextToken();
            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    fin_de_lectura = true;
                    break;
                case StreamTokenizer.TT_WORD:
                    if (readingTriangles) {
                        if (st.sval === "v") {
                            const p: Vector3Dd = new Vector3Dd(
                                this.readNumber(st),
                                this.readNumber(st),
                                this.readNumber(st),
                            );
                            triangleVertices.add(new Vertex(p));
                        } else if (st.sval === "f") {
                            const polygonIndices: ArrayList<number> = new ArrayList<number>();
                            for (;;) {
                                const faceToken: number = st.nextToken();
                                if (faceToken === StreamTokenizer.TT_EOL) {
                                    break;
                                }
                                if (faceToken === StreamTokenizer.TT_EOF) {
                                    fin_de_lectura = true;
                                    break;
                                }

                                let index: number;
                                if (faceToken === StreamTokenizer.TT_NUMBER) {
                                    index = Math.trunc(st.nval);
                                } else if (faceToken === StreamTokenizer.TT_WORD) {
                                    index = Integer.parseInt(st.sval as string);
                                } else {
                                    console.error("ERROR: face index expected in line " + st.lineno());
                                    throw new IOException(st.toString());
                                }
                                polygonIndices.add(index);
                            }
                            if (polygonIndices.size() < 3) {
                                console.error("ERROR: face with less than 3 vertices in line " + st.lineno());
                                throw new IOException(st.toString());
                            }
                            const anchor: number = polygonIndices.get(0);
                            for (let i = 1; i < polygonIndices.size() - 1; i++) {
                                const i1: number = polygonIndices.get(i);
                                const i2: number = polygonIndices.get(i + 1);
                                triangleFaces.add(new Triangle(anchor, i1, i2));
                            }
                        } else if (st.sval === "surface") {
                            this.flushTriangleBatch(
                                theScene,
                                triangleVertices,
                                triangleFaces,
                                currentTrianglesMaterial,
                                yaw_actual,
                                pitch_actual,
                                roll_actual,
                            );
                            triangleFaces.clear();
                            currentMaterial = this.readSurfaceDefinition(st);
                            currentTrianglesMaterial = currentMaterial;
                        } else if (st.sval === "end") {
                            this.flushTriangleBatch(
                                theScene,
                                triangleVertices,
                                triangleFaces,
                                currentTrianglesMaterial,
                                yaw_actual,
                                pitch_actual,
                                roll_actual,
                            );
                            triangleVertices.clear();
                            triangleFaces.clear();
                            readingTriangles = false;
                        } else {
                            console.error(
                                'ERROR: unsupported triangles token "' + st.sval + '" in line ' + st.lineno(),
                            );
                            throw new IOException(st.toString());
                        }
                        break;
                    }
                    if (readingQuads) {
                        if (st.sval === "v") {
                            const p: Vector3Dd = new Vector3Dd(
                                this.readNumber(st),
                                this.readNumber(st),
                                this.readNumber(st),
                            );
                            quadVertices.add(new Vertex(p));
                        } else if (st.sval === "q") {
                            const i0: number = Math.trunc(this.readNumber(st));
                            const i1: number = Math.trunc(this.readNumber(st));
                            const i2: number = Math.trunc(this.readNumber(st));
                            const i3: number = Math.trunc(this.readNumber(st));
                            quadFaces.add([i0, i1, i2, i3]);
                        } else if (st.sval === "surface") {
                            this.flushQuadBatch(
                                theScene,
                                quadVertices,
                                quadFaces,
                                currentQuadsMaterial,
                                yaw_actual,
                                pitch_actual,
                                roll_actual,
                            );
                            quadFaces.clear();
                            currentMaterial = this.readSurfaceDefinition(st);
                            currentQuadsMaterial = currentMaterial;
                        } else if (st.sval === "end") {
                            this.flushQuadBatch(
                                theScene,
                                quadVertices,
                                quadFaces,
                                currentQuadsMaterial,
                                yaw_actual,
                                pitch_actual,
                                roll_actual,
                            );
                            quadVertices.clear();
                            quadFaces.clear();
                            readingQuads = false;
                        } else {
                            console.error('ERROR: unsupported quads token "' + st.sval + '" in line ' + st.lineno());
                            throw new IOException(st.toString());
                        }
                        break;
                    }

                    if (st.sval === "sphere") {
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        const r: number = this.readNumber(st);

                        this.showDebugMessage("sphere");
                        thing = new SimpleBody();
                        thing.setGeometry(new Sphere(r));
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "cube") {
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        const r: number = this.readNumber(st);

                        this.showDebugMessage("cube");
                        thing = new SimpleBody();
                        thing.setGeometry(new Box(r, r, r));
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "cylinder") {
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        const r1: number = this.readNumber(st);
                        const r2: number = this.readNumber(st);
                        const h: number = this.readNumber(st);

                        this.showDebugMessage("cylinder");
                        thing = new SimpleBody();
                        thing.setGeometry(new Cone(r1, r2, h));
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "torus") {
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        const majorRadius: number = this.readNumber(st);
                        const minorRadius: number = this.readNumber(st);

                        this.showDebugMessage("torus");
                        thing = new SimpleBody();
                        thing.setGeometry(new Torus(majorRadius, minorRadius));
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "polybox") {
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        const sx: number = this.readNumber(st);
                        const sy: number = this.readNumber(st);
                        const sz: number = this.readNumber(st);

                        this.showDebugMessage("polybox");
                        const solid: PolyhedralBoundedSolid = new Box(sx, sy, sz).exportToPolyhedralBoundedSolid();
                        thing = new SimpleBody();
                        thing.setGeometry(solid);
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "appel") {
                        const appelId: number = Math.trunc(this.readNumber(st));
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        let uniformScale = 1.0;
                        const nextToken: number = st.nextToken();
                        if (nextToken === StreamTokenizer.TT_NUMBER) {
                            uniformScale = st.nval;
                        } else {
                            st.pushBack();
                        }
                        if (uniformScale <= 0) {
                            uniformScale = 1.0;
                        }

                        this.showDebugMessage("appel");
                        let solid: PolyhedralBoundedSolid;
                        if (appelId === 1) {
                            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_1();
                        } else if (appelId === 2) {
                            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_2();
                        } else {
                            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
                        }

                        // APPEL solids are defined in [0,1]^3; recenter for scene placement.
                        PolyhedralBoundedSolidModeler.applyTransformation(
                            solid,
                            new Matrix4x4d().translation(-0.5, -0.5, -0.5),
                        );

                        thing = new SimpleBody();
                        thing.setGeometry(solid);
                        thing.setMaterial(currentMaterial);
                        thing.setTexture(currentTexture);
                        thing.setNormalMap(currentNormalMap);
                        thing.setScale(new Vector3Dd(uniformScale, uniformScale, uniformScale));
                        this.applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                        theScene.addBody(thing);
                    } else if (st.sval === "obj") {
                        const objectPath: string = this.readStringToken(st);
                        const c: Vector3Dd = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        let uniformScale = 1.0;
                        const nextToken: number = st.nextToken();
                        if (nextToken === StreamTokenizer.TT_NUMBER) {
                            uniformScale = st.nval;
                        } else {
                            st.pushBack();
                        }
                        if (uniformScale <= 0) {
                            uniformScale = 1.0;
                        }
                        this.addImportedObj(
                            theScene,
                            objectPath,
                            currentMaterial,
                            yaw_actual,
                            pitch_actual,
                            roll_actual,
                            c,
                            uniformScale,
                        );
                    } else if (st.sval === "triangles") {
                        this.showDebugMessage("triangles");
                        readingTriangles = true;
                        triangleVertices.clear();
                        triangleFaces.clear();
                        currentTrianglesMaterial = currentMaterial;
                    } else if (st.sval === "quads") {
                        this.showDebugMessage("quads");
                        readingQuads = true;
                        quadVertices.clear();
                        quadFaces.clear();
                        currentQuadsMaterial = currentMaterial;
                    } else if (st.sval === "viewport") {
                        this.showDebugMessage("viewport");

                        context.viewportXSize = Math.trunc(this.readNumber(st));
                        context.viewportYSize = Math.trunc(this.readNumber(st));
                        this.configureCurrentCameraFromMitView(context);
                    } else if (st.sval === "eye") {
                        this.showDebugMessage("eye");
                        context.importedEye = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        this.configureCurrentCameraFromMitView(context);
                    } else if (st.sval === "lookat") {
                        this.showDebugMessage("lookat");
                        context.importedLookAt = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        this.configureCurrentCameraFromMitView(context);
                    } else if (st.sval === "up") {
                        this.showDebugMessage("up");
                        context.importedUp = new Vector3Dd(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                        this.configureCurrentCameraFromMitView(context);
                    } else if (st.sval === "fov") {
                        this.showDebugMessage("fov");
                        context.importedHorizontalFov = this.readNumber(st);
                        this.configureCurrentCameraFromMitView(context);
                    } else if (st.sval === "background") {
                        this.showDebugMessage("background");
                        context.currentBackground = new SimpleBackground();
                        (context.currentBackground as SimpleBackground).setColor(
                            this.readNumber(st),
                            this.readNumber(st),
                            this.readNumber(st),
                        );
                    } else if (st.sval === "backgroundcubemap") {
                        // Java loads the six cubemap faces through
                        // `ImagePersistence.importRGBA`. Raster image import is
                        // not ported to TypeScript yet, so this branch reports
                        // the missing capability instead of building a cubemap
                        // out of nothing. See the class header.
                        console.error("Error armando el cubemap!");
                        throw new IOException(
                            "backgroundcubemap needs raster image import, which is not ported to " +
                                "TypeScript yet (@vitral/fs ImagePersistence only exports images).",
                        );
                    } else if (st.sval === "light") {
                        this.showDebugMessage("light");
                        const r: number = this.readNumber(st);
                        const g: number = this.readNumber(st);
                        const b: number = this.readNumber(st);
                        if (st.nextToken() !== StreamTokenizer.TT_WORD) {
                            console.error("ERROR: in line " + st.lineno() + " at " + st.sval);
                            throw new IOException(st.toString());
                        }
                        // `st.sval` was narrowed to "light" by the enclosing
                        // branch; re-reading it into a local tells TypeScript
                        // that nextToken() has since replaced it.
                        const lightKind: string | undefined = st.sval;
                        if (lightKind === "ambient") {
                            this.showDebugMessage("ambient");
                            theScene.addLight(new AmbientLight(new ColorRgb(r, g, b)));
                        } else if (lightKind === "directional") {
                            this.showDebugMessage("directional");
                            const v: Vector3Dd = new Vector3Dd(
                                this.readNumber(st),
                                this.readNumber(st),
                                this.readNumber(st),
                            );
                            theScene.addLight(new DirectionalLight(v, new ColorRgb(r, g, b)));
                        } else if (lightKind === "point") {
                            this.showDebugMessage("point");
                            const v: Vector3Dd = new Vector3Dd(
                                this.readNumber(st),
                                this.readNumber(st),
                                this.readNumber(st),
                            );
                            theScene.addLight(new PointLight(v, new ColorRgb(r, g, b)));
                        } else {
                            console.error("ERROR: in line " + st.lineno() + " at " + st.sval);
                            throw new IOException(st.toString());
                        }
                    } else if (st.sval === "rotation") {
                        this.showDebugMessage("rotation");
                        yaw_actual = this.readNumber(st);
                        pitch_actual = this.readNumber(st);
                        roll_actual = this.readNumber(st);
                    } else if (st.sval === "surface") {
                        this.showDebugMessage("surface");
                        currentMaterial = this.readSurfaceDefinition(st);
                    } else if (st.sval === "texture") {
                        const texturePath: string = this.readStringToken(st);
                        currentTexture = this.loadRgbTexture(texturePath, textureCache);
                    } else if (st.sval === "notexture") {
                        currentTexture = null;
                    } else if (st.sval === "bumpmap") {
                        const bumpPath: string = this.readStringToken(st);
                        let bumpScale: Vector3Dd = new Vector3Dd(1, 1, 0.2);
                        const nextToken: number = st.nextToken();
                        if (nextToken === StreamTokenizer.TT_NUMBER) {
                            const sx: number = st.nval;
                            const sy: number = this.readNumber(st);
                            const sz: number = this.readNumber(st);
                            bumpScale = new Vector3Dd(sx, sy, sz);
                        } else {
                            st.pushBack();
                        }
                        currentNormalMap = this.loadNormalMapFromBump(bumpPath, bumpScale, normalMapCache);
                    } else if (st.sval === "nobumpmap") {
                        currentNormalMap = null;
                    } else {
                        console.error('ERROR: unsupported token "' + st.sval + '" in line ' + st.lineno());
                        throw new IOException(st.toString());
                    }
                    break;
                default:
                    console.error("ERROR: in line " + st.lineno() + " at " + st.sval);
                    throw new IOException(st.toString());
            } // switch
        } // while
        is.close();
        if (st.ttype !== StreamTokenizer.TT_EOF) {
            console.error("ERROR: in line " + st.lineno() + " at " + st.sval);
            throw new IOException(st.toString());
        }

        this.configureCurrentCameraFromMitView(context);

        theScene.addBackground(context.currentBackground);
        theScene.addCamera(context.currentCamera);
        theScene.setActiveCameraIndex(0);
        theScene.setActiveBackgroundIndex(0);
    }
}
