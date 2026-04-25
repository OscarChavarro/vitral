package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

/**
 * Ad-hoc console diagnostic for the MANT1988 15.2 holed UNION case.
 *
 * Run it directly from the command line after compiling test classes. It does
 * not change the kernel; it only exposes the Boolean pipeline state.
 */
public final class Mant1988Section15_2UnionDiagnostic
{
    private static final String[] FINAL_CUT_SOURCES = new String[] {
        "edgeRight", "edgeLeft", "h1", "h2", "h1Mirror", "h2Mirror"
    };

    private Mant1988Section15_2UnionDiagnostic()
    {
    }

    public static void main(String[] args) throws Exception
    {
        boolean preserveNullEdgeOrder =
            args != null && args.length > 0 && "nosort".equals(args[0]);
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        System.setProperty("vsdk.setop.tracePipelineSummary", "true");
        System.setProperty("vsdk.setop.traceCoplanarTangential", "true");
        if ( preserveNullEdgeOrder ) {
            System.setProperty("vsdk.setop.preserveNullEdgeOrder", "true");
        }

        System.out.println("preserveNullEdgeOrder=" + preserveNullEdgeOrder);
        printSolidSummary("operandA:block", solidA);
        printSolidSummary("operandB:wedge", solidB);

        prepareOperandsLikeSetOp(solidA, solidB);
        printSolidSummary("preparedA:block", solidA);
        printSolidSummary("preparedB:wedge", solidB);

        PolyhedralBoundedSolid preflight = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolid coplanarAreaResult =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runPartialCoplanarFaceAreaCase(
                    solidA, solidB, preflight,
                    PolyhedralBoundedSolidModeler.UNION);
        boolean touchingOnly =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runTouchingOnlyPreflightCase(solidA, solidB);

        System.out.println();
        System.out.println("=== Preflight ===");
        System.out.println("partialCoplanarAreaResult=" +
            (coplanarAreaResult != null));
        System.out.println("touchingOnlyPreflight=" + touchingOnly);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpGenerate",
            new Class<?>[] { PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            solidA, solidB);

        ArrayList<?> sonva = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonva");
        ArrayList<?> sonvb = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonvb");
        ArrayList<?> sonvv = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonvv");

        System.out.println();
        System.out.println("=== After Generate ===");
        System.out.println("sonva(vertex/face A) = " + sonva.size());
        System.out.println("sonvb(vertex/face B) = " + sonvb.size());
        System.out.println("sonvv(vertex/vertex) = " + sonvv.size());
        printList("sonva", sonva);
        printList("sonvb", sonvb);
        printList("sonvv", sonvv);

        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "sonea", sonea);
        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "soneb", soneb);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpClassify",
            new Class<?>[] { int.class, PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            PolyhedralBoundedSolidModeler.UNION, solidA, solidB);

        sonea = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonea");
        soneb = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "soneb");

        System.out.println();
        System.out.println("=== After Classify ===");
        System.out.println("sonea(null edges A) = " + sonea.size());
        System.out.println("soneb(null edges B) = " + soneb.size());
        printNullEdges("sonea", sonea);
        printNullEdges("soneb", soneb);
        printSolidSummary("classifiedA:block", solidA);
        printSolidSummary("classifiedB:wedge", solidB);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpConnect",
            new Class<?>[] { int.class },
            PolyhedralBoundedSolidModeler.UNION);

        ArrayList<_PolyhedralBoundedSolidFace> sonfa = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonfa");
        ArrayList<_PolyhedralBoundedSolidFace> sonfb = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonfb");

        System.out.println();
        System.out.println("=== After Connect ===");
        System.out.println("sonfa = " + sonfa.size());
        System.out.println("sonfb = " + sonfb.size());
        printFaces("sonfa", sonfa);
        printFaces("sonfb", sonfb);
        printSolidSummary("connectedA:block", solidA);
        printSolidSummary("connectedB:wedge", solidB);
        printShells("connectedA:block", solidA);
        printShells("connectedB:wedge", solidB);
        printSelectedFaces("connectedA:block", solidA, new int[] { 1, 4, 10, 14 });
        printSelectedFaces("connectedB:wedge", solidB, new int[] { 15, 21 });

        PolyhedralBoundedSolid result = new PolyhedralBoundedSolid();
        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpFinish",
            new Class<?>[] { PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class, PolyhedralBoundedSolid.class,
                int.class },
            solidA, solidB, result, PolyhedralBoundedSolidModeler.UNION);

        System.out.println();
        System.out.println("=== After Finish ===");
        printSolidSummary("result:finish", result);
        printShells("result:finish", result);
        printFaceLoops(result);
        printStrictLoopMessage(result);

        PolyhedralBoundedSolid finalResult =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1)[0];
        PolyhedralBoundedSolid finalOperandB =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1)[1];
        PolyhedralBoundedSolid union = PolyhedralBoundedSolidModeler.setOp(
            finalResult, finalOperandB, PolyhedralBoundedSolidModeler.UNION,
            false);

        System.out.println();
        System.out.println("=== Public setOp Result ===");
        printSolidSummary("result:publicSetOp", union);
        printShells("result:publicSetOp", union);
        printFaceLoops(union);
        printStrictLoopMessage(union);

        System.out.println();
        System.out.println("=== Finish Variants ===");
        printFinishVariant(false, false);
        printFinishVariant(true, false);
        printFinishVariant(false, true);
        printFinishVariant(true, true);

        System.out.println();
        System.out.println("=== Host Face Variants ===");
        printHostFaceVariant("hostFacesA[1,4]+Borig", new int[] { 1, 4 }, false);
        printHostFaceVariant("hostFacesA[1,4]+Bnew", new int[] { 1, 4 }, true);

        System.out.println();
        System.out.println("=== Final Cut Sweep ===");
        printFinalCutSweep();
    }

    private static void prepareOperandsLikeSetOp(PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB) throws Exception
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB);

        invokeProtectedStatic(_PolyhedralBoundedSolidOperator.class,
            "setNumericContext",
            new Class<?>[] {
                PolyhedralBoundedSolidNumericPolicy.ToleranceContext.class
            },
            numericContext);
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);

        solidA.compactIds();
        solidB.compactIds();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solidB);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solidA);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solidB);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solidB);
        solidA.compactIds();
        solidB.compactIds();
        PolyhedralBoundedSolidSetOperator.updmaxnames(solidB, solidA);

        numericContext = PolyhedralBoundedSolidNumericPolicy.forSolids(
            solidA, solidB);
        invokeProtectedStatic(_PolyhedralBoundedSolidOperator.class,
            "setNumericContext",
            new Class<?>[] {
                PolyhedralBoundedSolidNumericPolicy.ToleranceContext.class
            },
            numericContext);
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);
    }

    private static void printSolidSummary(String label,
        PolyhedralBoundedSolid solid)
    {
        System.out.println();
        System.out.println("=== " + label + " ===");
        System.out.println("faces=" + solid.polygonsList.size() +
            " edges=" + solid.edgesList.size() +
            " vertices=" + solid.verticesList.size() +
            " loops=" + computeLoopCount(solid) +
            " multiLoopFaces=" + computeMultiLoopFaceCount(solid) +
            " shells=" + computeShellCount(solid) +
            " strict=" + PolyhedralBoundedSolidValidationEngine
                .validateStrict(solid) +
            " intermediate=" + PolyhedralBoundedSolidValidationEngine
                .validateIntermediate(solid));
        System.out.println("shellFaceCounts=" + formatIntArray(
            computeShellFaceCounts(solid)));
        System.out.println("bbox=" + formatMinMax(solid.getMinMax()));
    }

    private static void printFaceLoops(PolyhedralBoundedSolid solid)
    {
        int i;
        int j;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            System.out.println("face[" + face.id + "] loops=" +
                face.boundariesList.size());
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                System.out.println("  loop[" + j + "] vertices=" +
                    face.boundariesList.get(j).halfEdgesList.size() +
                    " sequence=" +
                    summarizeLoop(face.boundariesList.get(j).halfEdgesList));
            }
        }
    }

    private static void printStrictLoopMessage(PolyhedralBoundedSolid solid)
    {
        StringBuilder msg = new StringBuilder();
        boolean loopsOk = PolyhedralBoundedSolidGeometricValidator
            .validateLoopsStrict(solid, msg);

        System.out.println("strictLoopsOk=" + loopsOk);
        if ( msg.length() > 0 ) {
            System.out.println(msg.toString().trim());
        }
    }

    private static void printFinishVariant(boolean useNewFacesA,
        boolean useNewFacesB) throws Exception
    {
        PolyhedralBoundedSolid result = runUnionFinishVariant(useNewFacesA,
            useNewFacesB);
        String label = "variant[A=" + (useNewFacesA ? "new" : "orig") +
            ",B=" + (useNewFacesB ? "new" : "orig") + "]";

        printSolidSummary(label, result);
        printShells(label, result);
        printStrictLoopMessage(result);
    }

    private static void printHostFaceVariant(String label, int[] faceIdsA,
        boolean useNewFacesB) throws Exception
    {
        try {
            PolyhedralBoundedSolid result =
                runUnionFinishUsingExplicitAFaces(faceIdsA, useNewFacesB);

            printSolidSummary(label, result);
            printShells(label, result);
            printStrictLoopMessage(result);
        }
        catch (Exception ex) {
            System.out.println(label + " ERROR " +
                ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static void printFinalCutSweep() throws Exception
    {
        int i;
        int j;
        for ( i = 0; i < FINAL_CUT_SOURCES.length; i++ ) {
            for ( j = 0; j < FINAL_CUT_SOURCES.length; j++ ) {
                printFinalCutVariant(FINAL_CUT_SOURCES[i], FINAL_CUT_SOURCES[j]);
            }
        }
    }

    private static void printFinalCutVariant(String sourceA, String sourceB)
        throws Exception
    {
        try {
            PolyhedralBoundedSolid result =
                runUnionWithFinalCutModes(sourceA, sourceB);

            System.out.println(
                "finalCut[A=" + sourceA + ",B=" + sourceB + "] " +
                "faces=" + result.polygonsList.size() +
                " edges=" + result.edgesList.size() +
                " vertices=" + result.verticesList.size() +
                " shells=" + computeShellCount(result) +
                " shellFaceCounts=" + formatIntArray(
                    computeShellFaceCounts(result)) +
                " bbox=" + formatMinMax(result.getMinMax()) +
                " strict=" + PolyhedralBoundedSolidValidationEngine
                    .validateStrict(result) +
                " intermediate=" + PolyhedralBoundedSolidValidationEngine
                    .validateIntermediate(result));
        }
        catch (Exception ex) {
            System.out.println(
                "finalCut[A=" + sourceA + ",B=" + sourceB + "] ERROR " +
                ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static void printList(String label, ArrayList<?> values)
    {
        int i;
        for ( i = 0; i < values.size(); i++ ) {
            System.out.println(label + "[" + i + "] = " + values.get(i));
        }
    }

    private static void printNullEdges(String label,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> values)
    {
        int i;
        for ( i = 0; i < values.size(); i++ ) {
            _PolyhedralBoundedSolidSetOperatorNullEdge edge = values.get(i);
            System.out.println(label + "[" + i + "] = " +
                summarizeEdge(edge.e));
        }
    }

    private static void printFaces(String label,
        ArrayList<_PolyhedralBoundedSolidFace> faces)
    {
        int i;
        for ( i = 0; i < faces.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = faces.get(i);
            System.out.println(label + "[" + i + "] faceId=" + face.id +
                " loops=" + face.boundariesList.size());
        }
    }

    private static void printSelectedFaces(String label,
        PolyhedralBoundedSolid solid, int[] faceIds)
    {
        int i;
        for ( i = 0; i < faceIds.length; i++ ) {
            _PolyhedralBoundedSolidFace face = findFaceById(solid, faceIds[i]);
            if ( face == null ) {
                System.out.println(label + " face[" + faceIds[i] + "] missing");
                continue;
            }
            System.out.println(label + " face[" + face.id + "] loops=" +
                face.boundariesList.size() + " shellFaceCount=" +
                computeShellFaceCount(solid, face));
        }
    }

    private static String summarizeEdge(_PolyhedralBoundedSolidEdge edge)
    {
        if ( edge == null ) {
            return "null";
        }
        return summarizeHalfEdge(edge.rightHalf) + " | " +
            summarizeHalfEdge(edge.leftHalf);
    }

    private static String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null ) {
            return "null";
        }
        return "he(v=" + he.startingVertex.id + "->" +
            he.next().startingVertex.id + ",f=" +
            he.parentLoop.parentFace.id + ")";
    }

    private static String summarizeLoop(
        vsdk.toolkit.common.CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge> halfEdges)
    {
        StringBuilder out = new StringBuilder();
        int i;
        for ( i = 0; i < halfEdges.size(); i++ ) {
            if ( i > 0 ) {
                out.append(" ");
            }
            out.append(halfEdges.get(i).startingVertex.id);
        }
        return out.toString();
    }

    private static int computeLoopCount(PolyhedralBoundedSolid solid)
    {
        int count = 0;
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            count += solid.polygonsList.get(i).boundariesList.size();
        }
        return count;
    }

    private static int computeMultiLoopFaceCount(PolyhedralBoundedSolid solid)
    {
        int count = 0;
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            if ( solid.polygonsList.get(i).boundariesList.size() > 1 ) {
                count++;
            }
        }
        return count;
    }

    private static int computeShellCount(PolyhedralBoundedSolid solid)
    {
        return computeShellFaceCounts(solid).length;
    }

    private static int computeShellFaceCount(PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace targetFace)
    {
        int faceCount = solid.polygonsList.size();
        if ( faceCount == 0 || targetFace == null ) {
            return 0;
        }

        DisjointSet dsu = new DisjointSet(faceCount);
        int i;

        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(i);
            if ( edge.leftHalf == null || edge.rightHalf == null ) {
                continue;
            }
            int indexA = faceIndexOf(solid, edge.leftHalf.parentLoop.parentFace);
            int indexB = faceIndexOf(solid, edge.rightHalf.parentLoop.parentFace);
            if ( indexA >= 0 && indexB >= 0 ) {
                dsu.union(indexA, indexB);
            }
        }

        int faceIndex = faceIndexOf(solid, targetFace);
        if ( faceIndex < 0 ) {
            return 0;
        }

        int targetRoot = dsu.find(faceIndex);
        int count = 0;
        for ( i = 0; i < faceCount; i++ ) {
            if ( dsu.find(i) == targetRoot ) {
                count++;
            }
        }
        return count;
    }

    private static int[] computeShellFaceCounts(PolyhedralBoundedSolid solid)
    {
        int faceCount = solid.polygonsList.size();
        if ( faceCount == 0 ) {
            return new int[0];
        }

        DisjointSet dsu = new DisjointSet(faceCount);
        int i;

        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(i);
            if ( edge.leftHalf == null || edge.rightHalf == null ) {
                continue;
            }
            int indexA = faceIndexOf(solid, edge.leftHalf.parentLoop.parentFace);
            int indexB = faceIndexOf(solid, edge.rightHalf.parentLoop.parentFace);
            if ( indexA >= 0 && indexB >= 0 ) {
                dsu.union(indexA, indexB);
            }
        }

        ArrayList<Integer> componentSizes = new ArrayList<Integer>();
        boolean[] rootSeen = new boolean[faceCount];
        int[] rootCounts = new int[faceCount];

        for ( i = 0; i < faceCount; i++ ) {
            int root = dsu.find(i);
            rootCounts[root]++;
        }
        for ( i = 0; i < faceCount; i++ ) {
            int root = dsu.find(i);
            if ( !rootSeen[root] ) {
                rootSeen[root] = true;
                componentSizes.add(Integer.valueOf(rootCounts[root]));
            }
        }

        Collections.sort(componentSizes);
        int[] out = new int[componentSizes.size()];
        for ( i = 0; i < componentSizes.size(); i++ ) {
            out[i] = componentSizes.get(i).intValue();
        }
        return out;
    }

    private static String formatIntArray(int[] values)
    {
        StringBuilder out = new StringBuilder();
        int i;
        out.append("[");
        for ( i = 0; i < values.length; i++ ) {
            if ( i > 0 ) {
                out.append(", ");
            }
            out.append(values[i]);
        }
        out.append("]");
        return out.toString();
    }

    private static int faceIndexOf(PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face)
    {
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            if ( solid.polygonsList.get(i) == face ) {
                return i;
            }
        }
        return -1;
    }

    private static String formatMinMax(double[] minMax)
    {
        StringBuilder out = new StringBuilder();
        int i;
        out.append("[");
        for ( i = 0; i < minMax.length; i++ ) {
            if ( i > 0 ) {
                out.append(", ");
            }
            out.append(minMax[i]);
        }
        out.append("]");
        return out.toString();
    }

    private static void printShells(String label, PolyhedralBoundedSolid solid)
    {
        int faceCount = solid.polygonsList.size();
        if ( faceCount == 0 ) {
            System.out.println("shells[" + label + "] = []");
            return;
        }

        DisjointSet dsu = new DisjointSet(faceCount);
        int i;
        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(i);
            if ( edge.leftHalf == null || edge.rightHalf == null ) {
                continue;
            }
            int indexA = faceIndexOf(solid, edge.leftHalf.parentLoop.parentFace);
            int indexB = faceIndexOf(solid, edge.rightHalf.parentLoop.parentFace);
            if ( indexA >= 0 && indexB >= 0 ) {
                dsu.union(indexA, indexB);
            }
        }

        LinkedHashMap<Integer, ArrayList<_PolyhedralBoundedSolidFace>> shells =
            new LinkedHashMap<Integer, ArrayList<_PolyhedralBoundedSolidFace>>();
        for ( i = 0; i < faceCount; i++ ) {
            int root = dsu.find(i);
            ArrayList<_PolyhedralBoundedSolidFace> faces = shells.get(root);
            if ( faces == null ) {
                faces = new ArrayList<_PolyhedralBoundedSolidFace>();
                shells.put(Integer.valueOf(root), faces);
            }
            faces.add(solid.polygonsList.get(i));
        }

        int shellIndex = 0;
        for ( Map.Entry<Integer, ArrayList<_PolyhedralBoundedSolidFace>> entry :
              shells.entrySet() ) {
            double[] bbox = faceListBoundingBox(entry.getValue());
            System.out.println("shell[" + label + "][" + shellIndex + "] faces=" +
                formatFaceIds(entry.getValue()) + " bbox=" + formatMinMax(bbox));
            shellIndex++;
        }
    }

    private static double[] faceListBoundingBox(
        ArrayList<_PolyhedralBoundedSolidFace> faces)
    {
        double[] bbox = new double[] {
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        };
        int i;
        int j;
        int k;
        for ( i = 0; i < faces.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = faces.get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                for ( k = 0; k < face.boundariesList.get(j).halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge he =
                        face.boundariesList.get(j).halfEdgesList.get(k);
                    updateBoundingBox(bbox, he.startingVertex.position.x(),
                        he.startingVertex.position.y(),
                        he.startingVertex.position.z());
                }
            }
        }
        return bbox;
    }

    private static void updateBoundingBox(double[] bbox, double x, double y,
        double z)
    {
        if ( x < bbox[0] ) {
            bbox[0] = x;
        }
        if ( y < bbox[1] ) {
            bbox[1] = y;
        }
        if ( z < bbox[2] ) {
            bbox[2] = z;
        }
        if ( x > bbox[3] ) {
            bbox[3] = x;
        }
        if ( y > bbox[4] ) {
            bbox[4] = y;
        }
        if ( z > bbox[5] ) {
            bbox[5] = z;
        }
    }

    private static String formatFaceIds(
        ArrayList<_PolyhedralBoundedSolidFace> faces)
    {
        StringBuilder out = new StringBuilder();
        int i;
        out.append("[");
        for ( i = 0; i < faces.size(); i++ ) {
            if ( i > 0 ) {
                out.append(", ");
            }
            out.append(faces.get(i).id);
        }
        out.append("]");
        return out.toString();
    }

    private static PolyhedralBoundedSolid runUnionFinishVariant(
        boolean useNewFacesA, boolean useNewFacesB) throws Exception
    {
        System.clearProperty("vsdk.setop.connectFinalCutSourceA");
        System.clearProperty("vsdk.setop.connectFinalCutSourceB");
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        prepareOperandsLikeSetOp(solidA, solidB);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpGenerate",
            new Class<?>[] { PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            solidA, solidB);

        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "sonea", new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>());
        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "soneb", new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>());

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpClassify",
            new Class<?>[] { int.class, PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            PolyhedralBoundedSolidModeler.UNION, solidA, solidB);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpConnect",
            new Class<?>[] { int.class },
            PolyhedralBoundedSolidModeler.UNION);

        ArrayList<_PolyhedralBoundedSolidFace> sonfa = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonfa");
        ArrayList<_PolyhedralBoundedSolidFace> sonfb = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonfb");

        int oldsize = sonfa.size();
        int i;
        for ( i = 0; i < oldsize; i++ ) {
            _PolyhedralBoundedSolidFace newFaceA =
                PolyhedralBoundedSolidEulerOperators.lmfkrh(solidA, sonfa.get(i).boundariesList.get(1),
                    solidA.getMaxFaceId() + 1);
            sonfa.add(newFaceA);

            _PolyhedralBoundedSolidFace newFaceB =
                PolyhedralBoundedSolidEulerOperators.lmfkrh(solidB, sonfb.get(i).boundariesList.get(1),
                    solidB.getMaxFaceId() + 1);
            sonfb.add(newFaceB);
        }

        int inda = useNewFacesA ? oldsize : 0;
        int indb = useNewFacesB ? oldsize : 0;
        PolyhedralBoundedSolid outRes = new PolyhedralBoundedSolid();

        for ( i = 0; i < oldsize; i++ ) {
            _PolyhedralBoundedSolidOperator.movefac(sonfa.get(i + inda), outRes);
            _PolyhedralBoundedSolidOperator.movefac(sonfb.get(i + indb), outRes);
        }

        _PolyhedralBoundedSolidOperator.cleanup(outRes);

        for ( i = 0; i < oldsize; i++ ) {
            PolyhedralBoundedSolidEulerOperators.lkfmrh(outRes, sonfa.get(i + inda), sonfb.get(i + indb));
            PolyhedralBoundedSolidTopologyEditing.loopGlue(outRes, sonfa.get(i + inda));
        }
        outRes.compactIds();

        return outRes;
    }

    private static PolyhedralBoundedSolid runUnionFinishUsingExplicitAFaces(
        int[] faceIdsA, boolean useNewFacesB) throws Exception
    {
        System.clearProperty("vsdk.setop.connectFinalCutSourceA");
        System.clearProperty("vsdk.setop.connectFinalCutSourceB");
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        prepareOperandsLikeSetOp(solidA, solidB);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpGenerate",
            new Class<?>[] { PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            solidA, solidB);

        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "sonea", new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>());
        setPrivateStaticField(PolyhedralBoundedSolidSetOperator.class,
            "soneb", new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>());

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpClassify",
            new Class<?>[] { int.class, PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class },
            PolyhedralBoundedSolidModeler.UNION, solidA, solidB);

        invokePrivateStatic(PolyhedralBoundedSolidSetOperator.class,
            "setOpConnect",
            new Class<?>[] { int.class },
            PolyhedralBoundedSolidModeler.UNION);

        ArrayList<_PolyhedralBoundedSolidFace> sonfb = getPrivateStaticList(
            PolyhedralBoundedSolidSetOperator.class, "sonfb");
        ArrayList<_PolyhedralBoundedSolidFace> sonfa =
            new ArrayList<_PolyhedralBoundedSolidFace>();
        int i;

        for ( i = 0; i < faceIdsA.length; i++ ) {
            sonfa.add(findFaceById(solidA, faceIdsA[i]));
        }

        int oldsize = sonfa.size();
        for ( i = 0; i < oldsize; i++ ) {
            if ( sonfa.get(i) == null || sonfa.get(i).boundariesList.size() < 2 ) {
                throw new IllegalStateException(
                    "Face A[" + faceIdsA[i] + "] has fewer than 2 loops after connect");
            }
            _PolyhedralBoundedSolidFace newFaceA =
                PolyhedralBoundedSolidEulerOperators.lmfkrh(solidA, sonfa.get(i).boundariesList.get(1),
                    solidA.getMaxFaceId() + 1);
            sonfa.add(newFaceA);

            _PolyhedralBoundedSolidFace newFaceB =
                PolyhedralBoundedSolidEulerOperators.lmfkrh(solidB, sonfb.get(i).boundariesList.get(1),
                    solidB.getMaxFaceId() + 1);
            sonfb.add(newFaceB);
        }

        int indb = useNewFacesB ? oldsize : 0;
        PolyhedralBoundedSolid outRes = new PolyhedralBoundedSolid();

        for ( i = 0; i < oldsize; i++ ) {
            _PolyhedralBoundedSolidOperator.movefac(sonfa.get(i), outRes);
            _PolyhedralBoundedSolidOperator.movefac(sonfb.get(i + indb), outRes);
        }

        _PolyhedralBoundedSolidOperator.cleanup(outRes);

        for ( i = 0; i < oldsize; i++ ) {
            PolyhedralBoundedSolidEulerOperators.lkfmrh(outRes, sonfa.get(i), sonfb.get(i + indb));
            PolyhedralBoundedSolidTopologyEditing.loopGlue(outRes, sonfa.get(i));
        }
        outRes.compactIds();

        return outRes;
    }

    private static _PolyhedralBoundedSolidFace findFaceById(
        PolyhedralBoundedSolid solid, int faceId)
    {
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            if ( solid.polygonsList.get(i).id == faceId ) {
                return solid.polygonsList.get(i);
            }
        }
        return null;
    }

    private static PolyhedralBoundedSolid runUnionWithFinalCutModes(
        String sourceA, String sourceB) throws Exception
    {
        boolean tracePipelineSummary =
            Boolean.getBoolean("vsdk.setop.tracePipelineSummary");

        System.setProperty("vsdk.setop.connectFinalCutSourceA", sourceA);
        System.setProperty("vsdk.setop.connectFinalCutSourceB", sourceB);
        System.setProperty("vsdk.setop.tracePipelineSummary", "false");
        try {
            PolyhedralBoundedSolid[] operands =
                PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
            PolyhedralBoundedSolid solidA = operands[0];
            PolyhedralBoundedSolid solidB = operands[1];

            return PolyhedralBoundedSolidModeler.setOp(
                solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);
        }
        finally {
            System.clearProperty("vsdk.setop.connectFinalCutSourceA");
            System.clearProperty("vsdk.setop.connectFinalCutSourceB");
            if ( tracePipelineSummary ) {
                System.setProperty("vsdk.setop.tracePipelineSummary", "true");
            }
            else {
                System.clearProperty("vsdk.setop.tracePipelineSummary");
            }
        }
    }

    private static void invokeProtectedStatic(Class<?> owner, String name,
        Class<?>[] parameterTypes, Object... args) throws Exception
    {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(null, args);
    }

    private static void invokePrivateStatic(Class<?> owner, String name,
        Class<?>[] parameterTypes, Object... args) throws Exception
    {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(null, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> ArrayList<T> getPrivateStaticList(Class<?> owner,
        String fieldName) throws Exception
    {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (ArrayList<T>)field.get(null);
    }

    private static void setPrivateStaticField(Class<?> owner, String fieldName,
        Object value) throws Exception
    {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class DisjointSet
    {
        private final int[] parent;

        private DisjointSet(int size)
        {
            parent = new int[size];
            int i;
            for ( i = 0; i < size; i++ ) {
                parent[i] = i;
            }
        }

        private int find(int value)
        {
            if ( parent[value] != value ) {
                parent[value] = find(parent[value]);
            }
            return parent[value];
        }

        private void union(int a, int b)
        {
            int rootA = find(a);
            int rootB = find(b);
            if ( rootA != rootB ) {
                parent[rootB] = rootA;
            }
        }
    }
}
