package vsdk.toolkit.io.geometry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.PersistenceElement;

public class ReaderPly extends PersistenceElement {
    private static final int FORMAT_ASCII = 0;
    private static final int FORMAT_BINARY_LITTLE_ENDIAN = 1;
    private static final int FORMAT_BINARY_BIG_ENDIAN = 2;

    private static class PlyProperty {
        boolean list;
        String name;
        String type;
        String countType;
        String valueType;
    }

    private static class PlyElement {
        String name;
        int count;
        ArrayList<PlyProperty> properties = new ArrayList<PlyProperty>();
    }

    private static class PlyHeader {
        int format = FORMAT_ASCII;
        ArrayList<PlyElement> elements = new ArrayList<PlyElement>();
    }

    private static class TriangleIndices {
        int a;
        int b;
        int c;

        TriangleIndices(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private static SimpleMaterial defaultMaterial() {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.18, 0.18, 0.18));
        m = m.withDiffuse(new ColorRgb(0.72, 0.76, 0.70));
        m = m.withSpecular(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDoubleSided(false);
        return m;
    }

    private static SimpleBody addThing(
        Geometry geometry,
        ArrayList<SimpleBody> bodies)
    {
        if ( geometry == null || bodies == null ) {
            return null;
        }

        SimpleBody thing = new SimpleBody();
        thing.setGeometry(geometry);
        thing.setPosition(new Vector3Dd());
        thing.setRotation(new Matrix4x4d());
        thing.setRotationInverse(new Matrix4x4d());
        thing.setMaterial(defaultMaterial());
        bodies.add(thing);
        return thing;
    }

    private static String readPlyAsciiLine(InputStream is) throws Exception {
        StringBuilder out = new StringBuilder();
        while ( true ) {
            int value = is.read();
            if ( value < 0 ) {
                break;
            }
            if ( value == '\n' ) {
                break;
            }
            if ( value != '\r' ) {
                out.append((char)value);
            }
        }
        return out.toString();
    }

    private static ArrayList<String> splitWhitespace(String line) {
        ArrayList<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(line);
        while ( tokenizer.hasMoreTokens() ) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

    private static PlyHeader readHeader(InputStream is) throws Exception {
        PlyHeader header = new PlyHeader();
        String line = readPlyAsciiLine(is);
        if ( !line.trim().equals("ply") ) {
            throw new IllegalArgumentException("input is not a PLY stream");
        }

        PlyElement currentElement = null;
        while ( true ) {
            line = readPlyAsciiLine(is);
            ArrayList<String> tokens = splitWhitespace(line);
            if ( tokens.isEmpty() ) {
                continue;
            }

            String tag = tokens.get(0).toLowerCase();
            if ( tag.equals("end_header") ) {
                return header;
            }
            if ( tag.equals("comment") || tag.equals("obj_info") ) {
                continue;
            }
            if ( tag.equals("format") && tokens.size() >= 2 ) {
                String format = tokens.get(1).toLowerCase();
                if ( format.equals("ascii") ) {
                    header.format = FORMAT_ASCII;
                }
                else if ( format.equals("binary_little_endian") ) {
                    header.format = FORMAT_BINARY_LITTLE_ENDIAN;
                }
                else if ( format.equals("binary_big_endian") ) {
                    header.format = FORMAT_BINARY_BIG_ENDIAN;
                }
                else {
                    throw new IllegalArgumentException("unsupported PLY format: " + format);
                }
                continue;
            }
            if ( tag.equals("element") && tokens.size() >= 3 ) {
                currentElement = new PlyElement();
                currentElement.name = tokens.get(1);
                currentElement.count = Integer.parseInt(tokens.get(2));
                header.elements.add(currentElement);
                continue;
            }
            if ( tag.equals("property") && currentElement != null ) {
                PlyProperty property = new PlyProperty();
                if ( tokens.size() >= 5 && tokens.get(1).equals("list") ) {
                    property.list = true;
                    property.countType = tokens.get(2);
                    property.valueType = tokens.get(3);
                    property.name = tokens.get(4);
                }
                else if ( tokens.size() >= 3 ) {
                    property.list = false;
                    property.type = tokens.get(1);
                    property.name = tokens.get(2);
                }
                currentElement.properties.add(property);
            }
        }
    }

    private static int plyTypeSize(String type) {
        if ( type.equals("char") || type.equals("int8") ||
             type.equals("uchar") || type.equals("uint8") ) {
            return 1;
        }
        if ( type.equals("short") || type.equals("int16") ||
             type.equals("ushort") || type.equals("uint16") ) {
            return 2;
        }
        if ( type.equals("int") || type.equals("int32") ||
             type.equals("uint") || type.equals("uint32") ||
             type.equals("float") || type.equals("float32") ) {
            return 4;
        }
        if ( type.equals("double") || type.equals("float64") ) {
            return 8;
        }
        return 0;
    }

    private static boolean isSignedIntegerType(String type) {
        return type.equals("char") || type.equals("int8") ||
               type.equals("short") || type.equals("int16") ||
               type.equals("int") || type.equals("int32");
    }

    private static boolean isFloatType(String type) {
        return type.equals("float") || type.equals("float32") ||
               type.equals("double") || type.equals("float64");
    }

    private static long unsignedToSigned(long value, int bytes) {
        if ( bytes == 1 && value >= 128L ) {
            return value - 256L;
        }
        if ( bytes == 2 && value >= 32768L ) {
            return value - 65536L;
        }
        if ( bytes == 4 && value >= 2147483648L ) {
            return value - 4294967296L;
        }
        return value;
    }

    private static long readUnsignedIntegerBytes(
        InputStream is,
        int bytes,
        boolean bigEndian) throws Exception
    {
        byte[] buffer = new byte[bytes];
        readBytes(is, buffer);

        long value = 0;
        if ( bigEndian ) {
            for ( int i = 0; i < bytes; i++ ) {
                value = (value << 8) | (buffer[i] & 0xffL);
            }
        }
        else {
            for ( int i = bytes - 1; i >= 0; i-- ) {
                value = (value << 8) | (buffer[i] & 0xffL);
            }
        }
        return value;
    }

    private static double readBinaryScalarAsDouble(
        InputStream is,
        String type,
        boolean bigEndian) throws Exception
    {
        int bytes = plyTypeSize(type);
        if ( bytes <= 0 ) {
            return 0.0;
        }

        long raw = readUnsignedIntegerBytes(is, bytes, bigEndian);
        if ( isFloatType(type) ) {
            if ( bytes == 4 ) {
                return Float.intBitsToFloat((int)raw);
            }
            return Double.longBitsToDouble(raw);
        }
        if ( isSignedIntegerType(type) ) {
            return (double)unsignedToSigned(raw, bytes);
        }
        return (double)raw;
    }

    private static int readBinaryScalarAsInt(
        InputStream is,
        String type,
        boolean bigEndian) throws Exception
    {
        return (int)readBinaryScalarAsDouble(is, type, bigEndian);
    }

    private static void skipBinaryScalar(InputStream is, String type) throws Exception {
        int bytes = plyTypeSize(type);
        if ( bytes > 0 ) {
            byte[] buffer = new byte[bytes];
            readBytes(is, buffer);
        }
    }

    private static boolean isFaceIndexProperty(String name) {
        return name.equals("vertex_indices") || name.equals("vertex_index");
    }

    private static void addFaceTriangles(
        List<Integer> indices,
        ArrayList<TriangleIndices> triangles)
    {
        if ( indices.size() < 3 ) {
            return;
        }
        for ( int i = 2; i < indices.size(); i++ ) {
            triangles.add(new TriangleIndices(
                indices.get(0),
                indices.get(i - 1),
                indices.get(i)));
        }
    }

    private static void readVertexAscii(
        PlyElement element,
        String line,
        ArrayList<Vector3Dd> vertices)
    {
        ArrayList<String> tokens = splitWhitespace(line);
        int tokenIndex = 0;
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for ( PlyProperty property : element.properties ) {
            if ( tokenIndex >= tokens.size() ) {
                break;
            }
            if ( property.list ) {
                int count = Integer.parseInt(tokens.get(tokenIndex++));
                tokenIndex += count;
                continue;
            }

            double value = Double.parseDouble(tokens.get(tokenIndex++));
            if ( property.name.equals("x") ) {
                x = value;
            }
            else if ( property.name.equals("y") ) {
                y = value;
            }
            else if ( property.name.equals("z") ) {
                z = value;
            }
        }
        vertices.add(new Vector3Dd(x, y, z));
    }

    private static void readFaceAscii(
        PlyElement element,
        String line,
        ArrayList<TriangleIndices> triangles)
    {
        ArrayList<String> tokens = splitWhitespace(line);
        int tokenIndex = 0;

        for ( PlyProperty property : element.properties ) {
            if ( tokenIndex >= tokens.size() ) {
                break;
            }
            if ( property.list ) {
                int count = Integer.parseInt(tokens.get(tokenIndex++));
                ArrayList<Integer> indices = new ArrayList<Integer>();
                for ( int j = 0; j < count && tokenIndex < tokens.size(); j++ ) {
                    int index = Integer.parseInt(tokens.get(tokenIndex++));
                    if ( isFaceIndexProperty(property.name) ) {
                        indices.add(index);
                    }
                }
                if ( isFaceIndexProperty(property.name) ) {
                    addFaceTriangles(indices, triangles);
                }
            }
            else {
                tokenIndex++;
            }
        }
    }

    private static void readVertexBinary(
        InputStream is,
        PlyElement element,
        boolean bigEndian,
        ArrayList<Vector3Dd> vertices) throws Exception
    {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for ( PlyProperty property : element.properties ) {
            if ( property.list ) {
                int count = readBinaryScalarAsInt(is, property.countType, bigEndian);
                for ( int j = 0; j < count; j++ ) {
                    skipBinaryScalar(is, property.valueType);
                }
                continue;
            }

            double value = readBinaryScalarAsDouble(is, property.type, bigEndian);
            if ( property.name.equals("x") ) {
                x = value;
            }
            else if ( property.name.equals("y") ) {
                y = value;
            }
            else if ( property.name.equals("z") ) {
                z = value;
            }
        }
        vertices.add(new Vector3Dd(x, y, z));
    }

    private static void readFaceBinary(
        InputStream is,
        PlyElement element,
        boolean bigEndian,
        ArrayList<TriangleIndices> triangles) throws Exception
    {
        for ( PlyProperty property : element.properties ) {
            if ( property.list ) {
                int count = readBinaryScalarAsInt(is, property.countType, bigEndian);
                ArrayList<Integer> indices = new ArrayList<Integer>();
                for ( int j = 0; j < count; j++ ) {
                    int index = readBinaryScalarAsInt(is, property.valueType, bigEndian);
                    if ( isFaceIndexProperty(property.name) ) {
                        indices.add(index);
                    }
                }
                if ( isFaceIndexProperty(property.name) ) {
                    addFaceTriangles(indices, triangles);
                }
            }
            else {
                skipBinaryScalar(is, property.type);
            }
        }
    }

    private static void skipBinaryElementRecord(
        InputStream is,
        PlyElement element,
        boolean bigEndian) throws Exception
    {
        for ( PlyProperty property : element.properties ) {
            if ( property.list ) {
                int count = readBinaryScalarAsInt(is, property.countType, bigEndian);
                for ( int j = 0; j < count; j++ ) {
                    skipBinaryScalar(is, property.valueType);
                }
            }
            else {
                skipBinaryScalar(is, property.type);
            }
        }
    }

    private static TriangleMesh buildTriangleMesh(
        ArrayList<Vector3Dd> vertices,
        ArrayList<TriangleIndices> triangles) throws Exception
    {
        if ( vertices.isEmpty() || triangles.isEmpty() ) {
            return null;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Vertex[]> vertexFuture = executorService.submit(new Callable<Vertex[]>() {
            public Vertex[] call() {
                Vertex[] out = new Vertex[vertices.size()];
                for ( int i = 0; i < vertices.size(); i++ ) {
                    out[i] = new Vertex(vertices.get(i));
                }
                return out;
            }
        });
        Future<Triangle[]> triangleFuture = executorService.submit(new Callable<Triangle[]>() {
            public Triangle[] call() {
                ArrayList<Triangle> out = new ArrayList<Triangle>(triangles.size());
                int vertexCount = vertices.size();
                for ( TriangleIndices t : triangles ) {
                    if ( t.a >= 0 && t.a < vertexCount &&
                         t.b >= 0 && t.b < vertexCount &&
                         t.c >= 0 && t.c < vertexCount ) {
                        out.add(new Triangle(t.a, t.b, t.c));
                    }
                }
                return out.toArray(new Triangle[out.size()]);
            }
        });

        Vertex[] meshVertices = vertexFuture.get();
        Triangle[] meshTriangles = triangleFuture.get();
        executorService.shutdownNow();

        if ( meshTriangles.length == 0 ) {
            return null;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.setName("PLY mesh");
        mesh.setVertexes(meshVertices, false, false, false, false);
        mesh.setTriangles(meshTriangles);
        mesh.calculateNormals();
        mesh.setMaterials(new SimpleMaterial[] { defaultMaterial() });
        mesh.setMaterialRanges(new int[][] { { 0, 0 } });
        return mesh;
    }

    private static void rotateLikeObj(ArrayList<Vector3Dd> vertices) {
        Matrix4x4d rotation =
            new Matrix4x4d().axisRotation(Math.PI / 2.0, new Vector3Dd(1, 0, 0));

        for ( int i = 0; i < vertices.size(); i++ ) {
            vertices.set(i, rotation.multiply(vertices.get(i)));
        }
    }

    public static ReaderPlyResult importGeometry(File inSceneFileFd) throws Exception {
        FileInputStream fis = new FileInputStream(inSceneFileFd);
        BufferedInputStream bis = new BufferedInputStream(fis);
        try {
            return importGeometry(bis);
        }
        finally {
            bis.close();
            fis.close();
        }
    }

    public static ReaderPlyResult importGeometry(InputStream is) throws Exception {
        PlyHeader header = readHeader(is);
        ArrayList<Vector3Dd> vertices = new ArrayList<Vector3Dd>();
        ArrayList<TriangleIndices> triangles = new ArrayList<TriangleIndices>();
        boolean binary = header.format != FORMAT_ASCII;
        boolean bigEndian = header.format == FORMAT_BINARY_BIG_ENDIAN;

        for ( PlyElement element : header.elements ) {
            if ( element.name.equals("vertex") ) {
                vertices.ensureCapacity(element.count);
            }

            for ( int i = 0; i < element.count; i++ ) {
                if ( binary ) {
                    if ( element.name.equals("vertex") ) {
                        readVertexBinary(is, element, bigEndian, vertices);
                    }
                    else if ( element.name.equals("face") ) {
                        readFaceBinary(is, element, bigEndian, triangles);
                    }
                    else {
                        skipBinaryElementRecord(is, element, bigEndian);
                    }
                }
                else {
                    String line = readPlyAsciiLine(is);
                    if ( element.name.equals("vertex") ) {
                        readVertexAscii(element, line, vertices);
                    }
                    else if ( element.name.equals("face") ) {
                        readFaceAscii(element, line, triangles);
                    }
                }
            }
        }

        rotateLikeObj(vertices);

        ReaderPlyResult result = new ReaderPlyResult();
        result.pointCloud = vertices;
        result.triangleMesh = buildTriangleMesh(vertices, triangles);
        return result;
    }

    public static void importEnvironment(
        File inSceneFileFd,
        SimpleScene inoutSimpleScene) throws Exception
    {
        ReaderPlyResult result = importGeometry(inSceneFileFd);
        if ( result.triangleMesh == null ) {
            Logger.reportMessage(null, VSDK.WARNING,
                "ReaderPly.importEnvironment",
                "PLY file contains no triangle mesh");
            return;
        }
        addThing(result.triangleMesh, inoutSimpleScene.getSimpleBodies());
    }
}
