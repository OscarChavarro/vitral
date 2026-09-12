/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
//=       private class _ReaderObjVertex and simplified TriangleMesh design =

// Java basic classes
import { ArrayList } from "@vitral/base/java/util/ArrayList";
import { BufferedReader } from "@vitral/base/java/io/BufferedReader";
import { HashMap } from "@vitral/base/java/util/HashMap";
import { Integer } from "@vitral/base/java/lang/Integer";
import { Double } from "@vitral/base/java/lang/Double";
import { Float } from "@vitral/base/java/lang/Float";
import { Math as JavaMath } from "@vitral/base/java/lang/Math";
import { StringTokenizer } from "@vitral/base/java/util/StringTokenizer";

// VitralSDK classes
import { ColorRgb } from "@vitral/base/vsdk/toolkit/common/color/ColorRgb";
import { Matrix4x4d } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Matrix4x4d";
import { Triangle } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Triangle";
import { Vertex } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Vertex";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { RGBAImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBAImageUncompressed";
import { Background } from "@vitral/base/vsdk/toolkit/environment/background/Background";
import { Camera } from "@vitral/base/vsdk/toolkit/environment/camera/Camera";
import { SimpleMaterial } from "@vitral/base/vsdk/toolkit/environment/material/SimpleMaterial";
import { Light } from "@vitral/base/vsdk/toolkit/environment/light/Light";
import { Geometry } from "@vitral/base/vsdk/toolkit/environment/geometry/Geometry";
import { TriangleMesh } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/TriangleMesh";
import { TriangleMeshGroup } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup";
import { SimpleBody } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleBody";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { File } from "../../../../java/io/File.js";
import { FileReader } from "../../../../java/io/FileReader.js";
import { PersistenceElement } from "../PersistenceElement.js";

//===========================================================================

/**
Class _ReaderObjVertex contains indexes to different Arrays inside a ReaderObj.
The objective of this class is to provide a temporary mapping between original
information in an Alias Wavefront Object file (.obj) and VSDK's TriangleMesh
representation. It is used only by the ReaderObj class for format conversion
of geometric data.
*/
class _ReaderObjVertex extends PersistenceElement {
    public vertexPositionIndex: number;
    public vertexNormalIndex: number;
    public vertexTextureCoordinateIndex: number;

    public constructor(other?: _ReaderObjVertex) {
        super();
        if (other === undefined) {
            this.vertexPositionIndex = -1;
            this.vertexNormalIndex = -1;
            this.vertexTextureCoordinateIndex = -1;
            return;
        }
        this.vertexPositionIndex = other.vertexPositionIndex;
        this.vertexTextureCoordinateIndex = other.vertexTextureCoordinateIndex;
        this.vertexNormalIndex = other.vertexNormalIndex;
    }

    /**
    Pending to check why Java ask for overloading of current method. Not clear
    how should be made.
    */
    public hashCode(): number {
        return this.vertexNormalIndex + 10 * this.vertexPositionIndex + 100 * this.vertexTextureCoordinateIndex;
    }

    public equals(alien: unknown): boolean {
        if (!(alien instanceof _ReaderObjVertex)) return false;
        const other: _ReaderObjVertex = alien;

        return !(
            other.vertexPositionIndex !== this.vertexPositionIndex ||
            other.vertexNormalIndex !== this.vertexNormalIndex ||
            other.vertexTextureCoordinateIndex !== this.vertexTextureCoordinateIndex
        );
    }

    public override toString(): string {
        return (
            "Vertex " +
            this.vertexPositionIndex +
            " / " +
            this.vertexNormalIndex +
            " / " +
            this.vertexTextureCoordinateIndex
        );
    }
}

//===========================================================================

/**
The class ReaderObj provides wavefront obj loading functionality. Wavefront
obj is a 3d object format used to describe polygon meshes; it is capable of
storing vertex, vertex normal, vertex texture, faces, material, texture and
other maps information.
By the use of extensions, it has the potential to describe more information.
The original Wavefront format is not well standarized, so many variations
could exist. This code currently manages only triangle faces, and interprets
other polygons to triangle fans.

\todo  Perhaps "ReaderObj" is not the best name for this class, as in the
future should support exporting (writing) operations. It could be renamed
to something as "PersistenceObj".
*/
export class ReaderObj extends PersistenceElement {
    /**
    This method reads an Alias/Wavefront .obj file in ASCII form from the
    given filename. A wavefront obj file can have many objects within, so this
    method returns a group of objects rather than a single one.

    Even though a wavefront obj file has many objects, all the objects in
    the file share a common set of vertexes; this loader only stores for a
    TriangleMesh the vertexes that it uses, not all the array of vertexes.

    For a mesh to have a material, the matrial file has to be in the same
    folder as the mesh file; the same statement can be given about the
    textures and other maps.

    \todo  should not recieve a filename, but a previously opened stream, to
    make it independent of filesystems, and generalize it to URLs or whatever
    other connection.
    */
    private static read(fileName: string): TriangleMeshGroup {
        //- Geometric data and geometric attributes extracted from file ---
        let vertexPositionsArray: ArrayList<Vector3Dd>;
        let vertexNormalsArray: ArrayList<Vector3Dd>;
        let vertexTextureCoordinatesArray: ArrayList<Vector3Dd>;

        vertexPositionsArray = new ArrayList<Vector3Dd>();
        vertexNormalsArray = new ArrayList<Vector3Dd>();
        vertexTextureCoordinatesArray = new ArrayList<Vector3Dd>();

        //- Topology data extracted from file -----------------------------
        // _ReaderObjVertex[] will always be of size 3 (3 vertexes groups)
        let triangleDatasetsArray: ArrayList<_ReaderObjVertex[]>;

        triangleDatasetsArray = new ArrayList<_ReaderObjVertex[]>();

        //- Accumulated states for currently builded geometric object -----
        let nextGeometricObjectName: string;
        let nextTexturesArray: ArrayList<RGBAImageUncompressed | null>;
        let nextMaterialsArray: ArrayList<SimpleMaterial | null>;

        nextGeometricObjectName = "OBJ_default_material";
        nextTexturesArray = new ArrayList<RGBAImageUncompressed | null>();
        nextMaterialsArray = new ArrayList<SimpleMaterial | null>();

        //- Aditional support data structures -----------------------------
        let meshGroup: ArrayList<TriangleMesh>;
        let texture_span_triangleRange_table: ArrayList<ArrayList<number[]>>;
        let auxInitialTextureMapping: ArrayList<number[]>;
        let material_triangleRange_table: ArrayList<number[]>;
        let texturesHashMap: HashMap<string, RGBAImageUncompressed | null>;
        let materialsHashMap: HashMap<string, SimpleMaterial>;
        let textureIndex: number;

        meshGroup = new ArrayList<TriangleMesh>();
        texturesHashMap = new HashMap<string, RGBAImageUncompressed | null>();
        materialsHashMap = new HashMap<string, SimpleMaterial>();
        textureIndex = 0;

        texture_span_triangleRange_table = new ArrayList<ArrayList<number[]>>();
        auxInitialTextureMapping = new ArrayList<number[]>();
        auxInitialTextureMapping.add([0, 0]);
        texture_span_triangleRange_table.add(auxInitialTextureMapping);

        material_triangleRange_table = new ArrayList<number[]>();

        //- Geometry object processing from file / control -------------------
        let br: BufferedReader;
        let lineOfText: string | undefined;

        br = new BufferedReader(new FileReader(fileName));

        while ((lineOfText = br.readLine()) !== undefined) {
            // Build material library
            if (lineOfText.startsWith("mtllib ")) {
                materialsHashMap = ReaderObj.readMaterials(lineOfText, fileName);
            }
            // Change active material
            if (lineOfText.startsWith("usemtl ")) {
                //
                let auxMaterialName: string;
                let auxStringTokenizer: StringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                auxMaterialName = auxStringTokenizer.nextToken().toCString();
                nextMaterialsArray.add(materialsHashMap.get(auxMaterialName) ?? null);
                //
                let auxMaterialRange: number[];
                auxMaterialRange = [0, 0];
                auxMaterialRange[0] = triangleDatasetsArray.size();
                auxMaterialRange[1] = nextMaterialsArray.size() - 1;
                material_triangleRange_table.add(auxMaterialRange);
            }
            // Add a vertex
            if (lineOfText.startsWith("v ")) {
                vertexPositionsArray.add(ReaderObj.readVertex(lineOfText));
            }
            // Add a normal
            if (lineOfText.startsWith("vn ")) {
                vertexNormalsArray.add(ReaderObj.readVertex(lineOfText));
            }
            // Add a texture coordinate
            if (lineOfText.startsWith("vt ")) {
                vertexTextureCoordinatesArray.add(ReaderObj.readVertexTexture(lineOfText));
            }
            // Read faces as triangles sets
            if (lineOfText.startsWith("f ")) {
                try {
                    // Note that only first 3 vertexes for each polygon are
                    // processed
                    let auxTriangleFanSet: ArrayList<_ReaderObjVertex[]>;
                    auxTriangleFanSet = ReaderObj.readPolygonAsTriangleFan(lineOfText);
                    let newVertexSet: _ReaderObjVertex[];

                    for (let i = 0; i < auxTriangleFanSet.size(); i++) {
                        newVertexSet = auxTriangleFanSet.get(i) as _ReaderObjVertex[];
                        triangleDatasetsArray.add(newVertexSet);
                    }

                    //
                    texture_span_triangleRange_table.get(textureIndex);
                    //int[] lastRange = actRanges.get(actRanges.size()-1);
                    //lastRange[1] = triangleDatasetsArray.size();
                } catch {
                    // NoSuchElementException
                }
            }
            // File specified textures management
            if (lineOfText.startsWith("usemap ")) {
                // Put texture in hash map or select it from hash map
                let auxTextureName: string;
                let auxStringTokenizer: StringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                auxTextureName = auxStringTokenizer.nextToken().toCString();
                if (!texturesHashMap.containsKey(auxTextureName)) {
                    let auxTexture: RGBAImageUncompressed | null;
                    auxTexture = ReaderObj.obtainTextureFromFile(lineOfText, fileName);
                    if (auxTexture === null) {
                        textureIndex = 0;
                    } else {
                        nextTexturesArray.add(auxTexture);
                        texture_span_triangleRange_table.add(new ArrayList<number[]>());
                        textureIndex = nextTexturesArray.size();
                    }
                    texturesHashMap.put(auxTextureName, auxTexture);
                } else {
                    const auxTexture: RGBAImageUncompressed | null = texturesHashMap.get(auxTextureName) ?? null;
                    if (auxTexture === null) {
                        textureIndex = 0;
                    } else if (!nextTexturesArray.contains(auxTexture)) {
                        nextTexturesArray.add(auxTexture);
                        texture_span_triangleRange_table.add(new ArrayList<number[]>());
                        textureIndex = nextTexturesArray.size();
                    } else {
                        textureIndex = nextTexturesArray.indexOf(auxTexture) + 1;
                    }
                }
                // Add selected texture to current object texture definition
                let actRanges: ArrayList<number[]>;
                actRanges = texture_span_triangleRange_table.get(textureIndex) as ArrayList<number[]>;
                const newRange: number[] = [0, 0];
                newRange[0] = triangleDatasetsArray.size();
                actRanges.add(newRange);
            }
            // Object building
            if (lineOfText.startsWith("o ") || lineOfText.startsWith("g ")) {
                if (vertexPositionsArray.size() > 0) {
                    ReaderObj.ensureMaterialSelection(
                        nextMaterialsArray,
                        materialsHashMap,
                        material_triangleRange_table,
                    );
                    ReaderObj.addMeshToGroup(
                        meshGroup,
                        nextGeometricObjectName,
                        vertexPositionsArray,
                        vertexNormalsArray,
                        vertexTextureCoordinatesArray,
                        triangleDatasetsArray,
                        nextTexturesArray,
                        texture_span_triangleRange_table,
                        nextMaterialsArray,
                        material_triangleRange_table,
                    );
                }

                // Process next object name
                let auxStringTokenizer: StringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                nextGeometricObjectName = auxStringTokenizer.nextToken().toCString();

                // Clear accumulated states variables
                if (vertexPositionsArray.size() > 0) {
                    nextTexturesArray = new ArrayList<RGBAImageUncompressed | null>();
                    nextMaterialsArray = new ArrayList<SimpleMaterial | null>();
                    triangleDatasetsArray = new ArrayList<_ReaderObjVertex[]>();
                    material_triangleRange_table = new ArrayList<number[]>();
                    texture_span_triangleRange_table = new ArrayList<ArrayList<number[]>>();
                    auxInitialTextureMapping = new ArrayList<number[]>();
                    const auxInitialRange: number[] = [0, 0];
                    auxInitialTextureMapping.add(auxInitialRange);
                    texture_span_triangleRange_table.add(auxInitialTextureMapping);
                    textureIndex = 0;
                }
            }
        }
        br.close();

        // Build the last mesh from remaining vertexes, if any
        if (vertexPositionsArray.size() > 0) {
            ReaderObj.ensureMaterialSelection(nextMaterialsArray, materialsHashMap, material_triangleRange_table);
            ReaderObj.addMeshToGroup(
                meshGroup,
                nextGeometricObjectName,
                vertexPositionsArray,
                vertexNormalsArray,
                vertexTextureCoordinatesArray,
                triangleDatasetsArray,
                nextTexturesArray,
                texture_span_triangleRange_table,
                nextMaterialsArray,
                material_triangleRange_table,
            );
        }

        //-----------------------------------------------------------------
        const finalTriangleMeshGroup: TriangleMeshGroup = new TriangleMeshGroup();
        let tm: TriangleMesh;
        let i: number;

        for (i = 0; i < meshGroup.size(); i++) {
            tm = meshGroup.get(i) as TriangleMesh;
            if (tm.getNumVertices() > 0) {
                finalTriangleMeshGroup.addMesh(tm);
            }
        }
        return finalTriangleMeshGroup;
    }

    private static ensureMaterialSelection(
        nextMaterialsArray: ArrayList<SimpleMaterial | null>,
        materialsHashMap: HashMap<string, SimpleMaterial>,
        material_triangleRange_table: ArrayList<number[]>,
    ): void {
        if (!nextMaterialsArray.isEmpty() || materialsHashMap.isEmpty()) {
            return;
        }
        const fallbackMaterial: SimpleMaterial = materialsHashMap.values()[0] as SimpleMaterial;
        nextMaterialsArray.add(fallbackMaterial);
        const auxMaterialRange: number[] = [0, 0];
        auxMaterialRange[0] = 0;
        auxMaterialRange[1] = 0;
        material_triangleRange_table.add(auxMaterialRange);
    }

    private static addMeshToGroup(
        meshGroup: ArrayList<TriangleMesh>,
        nextGeometricObjectName: string,
        vertexPositionsArray: ArrayList<Vector3Dd>,
        vertexNormalsArray: ArrayList<Vector3Dd>,
        vertexTextureCoordinatesArray: ArrayList<Vector3Dd>,
        triangleDatasetsArray: ArrayList<_ReaderObjVertex[]>,
        nextTexturesArray: ArrayList<RGBAImageUncompressed | null>,
        texture_span_triangleRange_table: ArrayList<ArrayList<number[]>>,
        nextMaterialsArray: ArrayList<SimpleMaterial | null>,
        material_triangleRange_table: ArrayList<number[]>,
    ): void {
        let i: number;
        let newTriangleMesh: TriangleMesh;
        newTriangleMesh = new TriangleMesh();

        //- If there are no specified materials, add a default one --------
        if (nextMaterialsArray.isEmpty()) {
            let m: SimpleMaterial;
            m = new SimpleMaterial();
            m = m.withName("default obj material");
            m = m.withDoubleSided(false);
            nextMaterialsArray.add(m);
        }

        //- Convert vertex data from obj format to VSDK format ------------
        let finalVertexes: ArrayList<_ReaderObjVertex>;
        let usedCombinedVertexes: HashMap<_ReaderObjVertex, number>;

        finalVertexes = new ArrayList<_ReaderObjVertex>();
        usedCombinedVertexes = new HashMap<_ReaderObjVertex, number>();
        let combinedVertexCount = 0;

        for (i = 0; i < triangleDatasetsArray.size(); i++) {
            const p1: _ReaderObjVertex = (triangleDatasetsArray.get(i) as _ReaderObjVertex[])[0] as _ReaderObjVertex;
            const p2: _ReaderObjVertex = (triangleDatasetsArray.get(i) as _ReaderObjVertex[])[1] as _ReaderObjVertex;
            const p3: _ReaderObjVertex = (triangleDatasetsArray.get(i) as _ReaderObjVertex[])[2] as _ReaderObjVertex;

            if (!usedCombinedVertexes.containsKey(p1)) {
                usedCombinedVertexes.put(p1, combinedVertexCount);
                combinedVertexCount++;
                finalVertexes.add(new _ReaderObjVertex(p1));
            }
            p1.vertexPositionIndex = usedCombinedVertexes.get(p1) as number;

            if (!usedCombinedVertexes.containsKey(p2)) {
                usedCombinedVertexes.put(p2, combinedVertexCount);
                combinedVertexCount++;
                finalVertexes.add(new _ReaderObjVertex(p2));
            }
            p2.vertexPositionIndex = usedCombinedVertexes.get(p2) as number;

            if (!usedCombinedVertexes.containsKey(p3)) {
                usedCombinedVertexes.put(p3, combinedVertexCount);
                combinedVertexCount++;
                finalVertexes.add(new _ReaderObjVertex(p3));
            }
            p3.vertexPositionIndex = usedCombinedVertexes.get(p3) as number;
        }

        //- Build the mesh vertexes ---------------------------------------
        let newVertexArray: Vertex[];
        let ti: number, ni: number;
        let p: Vector3Dd, n: Vector3Dd;
        let R: Matrix4x4d = new Matrix4x4d();

        R = R.axisRotation(JavaMath.toRadians(90), new Vector3Dd(1, 0, 0));
        newVertexArray = new Array<Vertex>(finalVertexes.size());
        for (i = 0; i < finalVertexes.size(); i++) {
            // Position
            let srcPos: number = ReaderObj.resolveObjIndex(
                (finalVertexes.get(i) as _ReaderObjVertex).vertexPositionIndex,
                vertexPositionsArray.size(),
            );
            if (srcPos < 0 || srcPos >= vertexPositionsArray.size()) {
                srcPos = 0;
            }
            p = vertexPositionsArray.get(srcPos) as Vector3Dd;
            p = R.multiply(p);
            newVertexArray[i] = new Vertex(p);
            // Texture coordinates
            ti = ReaderObj.resolveObjIndex(
                (finalVertexes.get(i) as _ReaderObjVertex).vertexTextureCoordinateIndex,
                vertexTextureCoordinatesArray.size(),
            );
            if (ti >= 0 && ti < vertexTextureCoordinatesArray.size()) {
                (newVertexArray[i] as Vertex).u = (vertexTextureCoordinatesArray.get(ti) as Vector3Dd).x();
                (newVertexArray[i] as Vertex).v = (vertexTextureCoordinatesArray.get(ti) as Vector3Dd).y();
            } else {
                (newVertexArray[i] as Vertex).u = (newVertexArray[i] as Vertex).v = 0.0;
            }
            // Normals
            ni = ReaderObj.resolveObjIndex(
                (finalVertexes.get(i) as _ReaderObjVertex).vertexNormalIndex,
                vertexNormalsArray.size(),
            );
            if (ni >= 0 && ni < vertexNormalsArray.size()) {
                n = vertexNormalsArray.get(ni) as Vector3Dd;
                n = R.multiply(n);
                (newVertexArray[i] as Vertex).setNormal(n);
            } else {
                (newVertexArray[i] as Vertex).setNormal(new Vector3Dd(0, 0, 0));
            }
        }
        newTriangleMesh.setVertexes(newVertexArray, true, false, false, true);

        //- Build the mesh triangles --------------------------------------
        let newTriangleArray: Triangle[];

        newTriangleArray = new Array<Triangle>(triangleDatasetsArray.size());
        for (i = 0; i < newTriangleArray.length; i++) {
            newTriangleArray[i] = new Triangle();
            (newTriangleArray[i] as Triangle).setPoint0(
                ((triangleDatasetsArray.get(i) as _ReaderObjVertex[])[0] as _ReaderObjVertex).vertexPositionIndex,
            );
            (newTriangleArray[i] as Triangle).setPoint1(
                ((triangleDatasetsArray.get(i) as _ReaderObjVertex[])[1] as _ReaderObjVertex).vertexPositionIndex,
            );
            (newTriangleArray[i] as Triangle).setPoint2(
                ((triangleDatasetsArray.get(i) as _ReaderObjVertex[])[2] as _ReaderObjVertex).vertexPositionIndex,
            );
        }
        newTriangleMesh.setTriangles(newTriangleArray);

        //- Process materials ---------------------------------------------
        let materials: SimpleMaterial[];
        materials = new Array<SimpleMaterial>(nextMaterialsArray.size());

        for (i = 0; i < materials.length; i++) {
            materials[i] = nextMaterialsArray.get(i) as SimpleMaterial;
            if (materials[i] === null || materials[i] === undefined) {
                materials[i] = new SimpleMaterial();
                materials[i] = (materials[i] as SimpleMaterial).withDoubleSided(false);
            }
        }
        newTriangleMesh.setMaterials(materials);

        //- Process material ranges ---------------------------------------
        let auxMaterialRange: number[];
        auxMaterialRange = [0, 0];
        auxMaterialRange[0] = triangleDatasetsArray.size();
        auxMaterialRange[1] = nextMaterialsArray.size() - 1;
        material_triangleRange_table.add(auxMaterialRange);

        let materialRanges: number[][];

        materialRanges = [];
        for (i = 0; i < material_triangleRange_table.size(); i++) {
            materialRanges.push([0, 0]);
        }
        for (i = 1; i < material_triangleRange_table.size(); i++) {
            (materialRanges[i] as number[])[0] = (material_triangleRange_table.get(i) as number[])[0] as number;
            (materialRanges[i] as number[])[1] = (material_triangleRange_table.get(i - 1) as number[])[1] as number;
        }
        newTriangleMesh.setMaterialRanges(materialRanges);

        //- Process textures ----------------------------------------------
        let newTextureArray: RGBAImageUncompressed[];

        newTextureArray = new Array<RGBAImageUncompressed>(nextTexturesArray.size());
        for (i = 0; i < newTextureArray.length; i++) {
            newTextureArray[i] = nextTexturesArray.get(i) as RGBAImageUncompressed;
        }
        newTriangleMesh.setTextures(newTextureArray);

        //- Process texture ranges ----------------------------------------
        let numTextureSpans = 0;
        for (let textureIndex = 0; textureIndex < texture_span_triangleRange_table.size(); textureIndex++) {
            for (
                let j = 0;
                j < (texture_span_triangleRange_table.get(textureIndex) as ArrayList<number[]>).size();
                j++
            ) {
                numTextureSpans++;
            }
        }

        const textureRanges: number[][] = [];
        for (i = 0; i < numTextureSpans; i++) {
            textureRanges.push([0, 0]);
        }
        i = 0;
        for (let textureIndex = 0; textureIndex < texture_span_triangleRange_table.size(); textureIndex++) {
            for (
                let j = 0;
                j < (texture_span_triangleRange_table.get(textureIndex) as ArrayList<number[]>).size();
                j++
            ) {
                (textureRanges[i] as number[])[0] = (
                    (texture_span_triangleRange_table.get(textureIndex) as ArrayList<number[]>).get(j) as number[]
                )[1] as number;
                (textureRanges[i] as number[])[1] = textureIndex;
                i++;
            }
        }
        if (textureRanges.length > 0) {
            ReaderObj.quickSortTriangleRange(textureRanges, 0, textureRanges.length - 1);
        }
        newTriangleMesh.setTextureRanges(textureRanges);

        //- Finalize mesh and add to group --------------------------------
        if (vertexNormalsArray.size() < 0) {
            newTriangleMesh.calculateNormals();
        }
        newTriangleMesh.setName(nextGeometricObjectName);
        meshGroup.add(newTriangleMesh);
    }

    private static quickSortTriangleRange(a: number[][], izq: number, der: number): void {
        let i = izq;
        let j = der;
        const pivote = (a[Math.trunc((izq + der) / 2)] as number[])[0] as number;
        let aux0: number;
        let aux1: number;
        do {
            while (((a[i] as number[])[0] as number) < pivote) i++;
            while (((a[j] as number[])[0] as number) > pivote) j--;
            if (i <= j) {
                aux0 = (a[i] as number[])[0] as number;
                (a[i] as number[])[0] = (a[j] as number[])[0] as number;
                (a[j] as number[])[0] = aux0;
                aux1 = (a[i] as number[])[1] as number;
                (a[i] as number[])[1] = (a[j] as number[])[1] as number;
                (a[j] as number[])[1] = aux1;
                i++;
                j--;
            }
        } while (i <= j);
        if (izq < j) ReaderObj.quickSortTriangleRange(a, izq, j);
        if (i < der) ReaderObj.quickSortTriangleRange(a, i, der);
    }

    /**
    Java reaches the referenced map through `ImagePersistence.importRGBA`,
    and answers null whenever that import fails. Raster image *import* has no
    port yet in the TypeScript edition (only export does), so this method
    currently always takes Java's failure branch. Once an importer lands in
    `@vitral/fs`, this is the single place to wire it in.
    */
    private static obtainTextureFromFile(lineOfText: string, fileName: string): RGBAImageUncompressed | null {
        const st = new StringTokenizer(lineOfText, " ");
        st.nextToken(); //usemap
        const nomImage = st.nextToken().toCString();
        if (nomImage === "(null)") {
            return null;
        }
        void fileName;
        return null;
    }

    /**
    This method reads a polygon from a face line. It returns a set of triangles
    as an ArrayList of matrices. For each matrix, there is the information of
    a single triangle, where there are 3 vertexes with: vertex position index,
    texture coordinates index and vertex normal index.
    Note that the triangle set is builded as a triangle fan: the first vertex
    (p0) is a pivot which is fixed for all triangles, the second point
    determines the first triangle edge, and for each following vertex,
    a new triangle is builded.
    */
    private static readPolygonAsTriangleFan(lineOfText: string): ArrayList<_ReaderObjVertex[]> {
        let ret: ArrayList<_ReaderObjVertex[]>;
        const st = new StringTokenizer(lineOfText, " \n\r\t");
        st.nextToken(); // The "f" token
        const numberOfTokens = st.countTokens();
        let p0: _ReaderObjVertex | null = null;
        let p1: _ReaderObjVertex | null = null;
        let p2: _ReaderObjVertex;
        let aux: _ReaderObjVertex[];
        let i: number;

        ret = new ArrayList<_ReaderObjVertex[]>();
        for (i = 0; i < numberOfTokens; i++) {
            const token = st.nextToken().toCString();
            const indexes = ReaderObj.readFaceVertex(token);

            if (i === 0) {
                p0 = indexes;
            } else if (i === 1) {
                p1 = indexes;
            } else {
                p2 = indexes;

                aux = new Array<_ReaderObjVertex>(3);
                aux[0] = new _ReaderObjVertex(p0 as _ReaderObjVertex);
                aux[1] = new _ReaderObjVertex(p1 as _ReaderObjVertex);
                aux[2] = new _ReaderObjVertex(p2);
                ret.add(aux);

                p1 = new _ReaderObjVertex(p2);
            }
        }
        return ret;
    }

    /**
    Parses integer index values preserving sign (OBJ supports negative indices).
    */
    private static readIndexInteger(inToken: string): number {
        return Integer.parseInt(inToken);
    }

    private static resolveObjIndex(rawIndex: number, count: number): number {
        if (rawIndex > 0) {
            return rawIndex - 1;
        }
        if (rawIndex < 0) {
            return count + rawIndex;
        }
        return -1;
    }

    /**
    Returns three indices: vertex position, texture coordinates and normal,
    as a vertex
    */
    private static readFaceVertex(lineOfText: string): _ReaderObjVertex {
        const ret = new _ReaderObjVertex();

        const st = new StringTokenizer(lineOfText, "/");

        if (st.countTokens() === 2) {
            if (lineOfText.endsWith("/")) {
                // Has vertex and texture
                try {
                    ret.vertexPositionIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
                } catch {
                    ret.vertexPositionIndex = -1;
                }
                try {
                    ret.vertexTextureCoordinateIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
                } catch {
                    ret.vertexTextureCoordinateIndex = -1;
                }
                ret.vertexNormalIndex = -1;
            } else {
                // Has vertex and normal
                try {
                    ret.vertexPositionIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
                } catch {
                    ret.vertexPositionIndex = -1;
                }
                ret.vertexTextureCoordinateIndex = -1;
                try {
                    ret.vertexNormalIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
                } catch {
                    ret.vertexNormalIndex = -1;
                }
            }
        } else {
            // Has all
            try {
                ret.vertexPositionIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
            } catch {
                ret.vertexPositionIndex = -1;
            }
            try {
                ret.vertexTextureCoordinateIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
            } catch {
                ret.vertexTextureCoordinateIndex = -1;
            }
            try {
                ret.vertexNormalIndex = ReaderObj.readIndexInteger(st.nextToken().toCString());
            } catch {
                ret.vertexNormalIndex = -1;
            }
        }
        return ret;
    }

    private static readVertex(lineOfText: string): Vector3Dd {
        let vert: Vector3Dd;
        const st = new StringTokenizer(lineOfText);
        st.nextToken();
        vert = new Vector3Dd(
            Double.parseDouble(st.nextToken().toCString()),
            Double.parseDouble(st.nextToken().toCString()),
            Double.parseDouble(st.nextToken().toCString()),
        );

        return vert;
    }

    private static readVertexTexture(lineOfText: string): Vector3Dd {
        let vert: Vector3Dd;
        const st = new StringTokenizer(lineOfText);
        st.nextToken();
        const x = Double.parseDouble(st.nextToken().toCString());
        const y = Double.parseDouble(st.nextToken().toCString());
        let z = 0.0;
        try {
            z = Double.parseDouble(st.nextToken().toCString());
        } catch {
            /* empty */
        }
        vert = new Vector3Dd(x, y, z);
        return vert;
    }

    private static readMaterials(material: string, fileName: string): HashMap<string, SimpleMaterial> {
        const ret = new HashMap<string, SimpleMaterial>();
        const st = new StringTokenizer(material, " ");
        st.nextToken(); // "mtlib" token
        const arc = new File(fileName);
        const dirArc = arc.getParentFile();
        let nomArc: string;
        nomArc = String(dirArc) + File.separator + st.nextToken().toCString();

        try {
            const inReader = new BufferedReader(new FileReader(nomArc));
            let lineOfText: string | undefined;

            let activeMaterial: SimpleMaterial = new SimpleMaterial();
            activeMaterial = activeMaterial.withDoubleSided(false);
            activeMaterial = activeMaterial.withName("default");

            while ((lineOfText = inReader.readLine()) !== undefined) {
                if (lineOfText.startsWith("Ns")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ns
                    activeMaterial = activeMaterial.withPhongExponent(Float.parseFloat(stMat.nextToken().toCString()));
                }
                if (lineOfText.startsWith("d ")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // d
                    activeMaterial = activeMaterial.withOpacity(Float.parseFloat(stMat.nextToken().toCString()));
                }
                if (lineOfText.startsWith("Tr ")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Tr
                    activeMaterial = activeMaterial.withOpacity(1.0 - Float.parseFloat(stMat.nextToken().toCString()));
                }
                if (lineOfText.startsWith("Kd")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Kd
                    const color = new ColorRgb(
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                    activeMaterial = activeMaterial.withDiffuse(color);
                }
                if (lineOfText.startsWith("Ka")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ka
                    const color = new ColorRgb(
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                    activeMaterial = activeMaterial.withAmbient(color);
                }
                if (lineOfText.startsWith("Ks")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ks
                    const color = new ColorRgb(
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                    activeMaterial = activeMaterial.withSpecular(color);
                }
                if (lineOfText.startsWith("Ke")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ke
                    const color = new ColorRgb(
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                    activeMaterial = activeMaterial.withEmission(color);
                }
                if (lineOfText.startsWith("Kt") || lineOfText.startsWith("Tf")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Kt/Tf
                    const color = new ColorRgb(
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                    activeMaterial = activeMaterial.withTransmittance(color);
                }
                if (lineOfText.startsWith("Ni")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ni
                    activeMaterial = activeMaterial.withIndexOfRefraction(
                        Float.parseFloat(stMat.nextToken().toCString()),
                    );
                }
                if (lineOfText.startsWith("illum")) {
                    // Ignored by Vitral material model.
                }
                if (lineOfText.startsWith("newmtl")) {
                    const stMat = new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); //newmtl
                    ret.put(activeMaterial.getName(), activeMaterial);
                    activeMaterial = new SimpleMaterial();
                    activeMaterial = activeMaterial.withDoubleSided(false);
                    activeMaterial = activeMaterial.withName(stMat.nextToken().toCString());
                }
            }
            inReader.close();
            ret.put(activeMaterial.getName(), activeMaterial);
        } catch {
            // IOException
        }
        return ret;
    }

    private static defaultMaterial(): SimpleMaterial {
        let m: SimpleMaterial = new SimpleMaterial();

        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withDoubleSided(false);
        return m;
    }

    private static addThing(g: Geometry, inoutSimpleBodiesArray: ArrayList<SimpleBody> | null): void {
        if (inoutSimpleBodiesArray === null) return;

        let thing: SimpleBody;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3Dd());
        thing.setRotation(new Matrix4x4d());
        thing.setRotationInverse(new Matrix4x4d());
        thing.setMaterial(ReaderObj.defaultMaterial());
        inoutSimpleBodiesArray.add(thing);
    }

    public static importEnvironment(inSceneFileFd: File, inoutSimpleScene: SimpleScene): void {
        //-----------------------------------------------------------------
        const simpleBodiesArray: ArrayList<SimpleBody> = inoutSimpleScene.getSimpleBodies();
        const lightsArray: ArrayList<Light> = inoutSimpleScene.getLights();
        const backgroundsArray: ArrayList<Background> = inoutSimpleScene.getBackgrounds();
        const camerasArray: ArrayList<Camera> = inoutSimpleScene.getCameras();
        void lightsArray;
        void backgroundsArray;
        void camerasArray;

        //-----------------------------------------------------------------
        let mg: TriangleMeshGroup;
        mg = ReaderObj.read(inSceneFileFd.getAbsolutePath().toCString());
        ReaderObj.addThing(mg, simpleBodiesArray);
    }
}
