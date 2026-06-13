import models.AppelDisplayMode;
import models.DebuggerModel;
import models.SolidModelNames;
import models.CsgSampleNames;
import gui.CameraFaceFocusInteraction;
import options.CommandLineOptions;
import render.Jogl4HeadlessRenderer;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PolyhedralBoundedSolidExample
{
    public static void main(String[] args)
    {
        DebuggerModel model = new DebuggerModel();
        CommandLineOptions options;

        try {
            options = CommandLineOptions.parse(args);
        }
        catch ( IllegalArgumentException e ) {
            System.err.println("[PolyhedralBoundedSolidExample] " + e.getMessage());
            System.err.println(
                "Usage: [--offline] [--screenshot <file.png>] [--faceId <id>] [--edgeIndex <id>] [--appelDump <file.json>]");
            return;
        }

        // Keep the debugger process alive and surface fatal kernel issues as exceptions.
        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);

        configureInitialModelFromSystemProperty(model);
        applyOptionOverrides(model, options);
        buildSolidWithRecovery(model);

        if ( options.isMotifSweep() ) {
            runMotifSweep(model, options.getOutputPath());
            return;
        }
        if ( options.isOffline() ) {
            try {
                applyOfflineFaceSelection(model, options);
            }
            catch ( RuntimeException e ) {
                System.err.println("[PolyhedralBoundedSolidExample] " + e.getMessage());
                System.exit(2);
                return;
            }
            applyOfflineAppelDisplayMode(model);
            Jogl4HeadlessRenderer renderer = new Jogl4HeadlessRenderer(
                model, new File(options.getOutputPath()));
            renderer.render();
            exportAppelDumpIfRequested(model, options);
            if ( model.isErrorState() ) {
                System.err.println("[PolyhedralBoundedSolidExample] BUILD-ERROR: "
                    + model.getErrorMessage());
                System.exit(2);
            }
            return;
        }

        InteractiveDebugger.launch(model);
    }

    private static void applyOfflineFaceSelection(
        DebuggerModel model,
        CommandLineOptions options)
    {
        Integer requestedFaceId = options.getFaceId();
        int faceIndex;
        _PolyhedralBoundedSolidFace face;

        if ( requestedFaceId == null ) {
            return;
        }

        faceIndex = findFaceIndexById(model.getSolid(), requestedFaceId.intValue());
        if ( faceIndex < 0 ) {
            throw new IllegalArgumentException(
                "Face id " + requestedFaceId + " not found in current solid");
        }
        model.setFaceIndex(faceIndex);
        model.clampFaceIndex();

        if ( !(new CameraFaceFocusInteraction()).focusSelectedFace(model) ) {
            throw new IllegalStateException(
                "Cannot focus camera on selected face id " + requestedFaceId);
        }

        face = model.getSolid().getPolygonsList().get(faceIndex);
        System.out.println("[PolyhedralBoundedSolidExample] Selected face id=" +
            face.id + " index=" + faceIndex + " loops=" + face.boundariesList.size());
    }

    /**
    Offline override for the Appel hidden-line display mode (the state cycled
    by key [8] in the interactive debugger), via -Dpoly.appelDisplayMode. The
    value may be an ordinal (0=OFF, 1=edges+visible+hidden, 2=edges+visible) or
    the enum constant name. Useful for scripting per-state screenshots.

    @param model debugger model whose display mode is overridden
    */
    private static void applyOfflineAppelDisplayMode(DebuggerModel model)
    {
        String value = System.getProperty("poly.appelDisplayMode");
        if ( value == null || value.isBlank() ) {
            return;
        }
        value = value.trim();

        AppelDisplayMode[] modes = AppelDisplayMode.values();
        try {
            int ordinal = Integer.parseInt(value);
            if ( ordinal >= 0 && ordinal < modes.length ) {
                model.setAppelDisplayMode(modes[ordinal]);
                return;
            }
        }
        catch ( NumberFormatException e ) {
            // Not an ordinal; fall through to name parsing.
        }

        try {
            model.setAppelDisplayMode(AppelDisplayMode.valueOf(value));
        }
        catch ( IllegalArgumentException e ) {
            System.err.println("[PolyhedralBoundedSolidExample] Ignoring unknown "
                + "poly.appelDisplayMode='" + value + "'");
        }
    }

    private static int findFaceIndexById(PolyhedralBoundedSolid solid, int faceId)
    {
        if ( solid == null || solid.getPolygonsList() == null ) {
            return -1;
        }
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            if ( solid.getPolygonsList().get(i).id == faceId ) {
                return i;
            }
        }
        return -1;
    }

    private static void configureInitialModelFromSystemProperty(DebuggerModel model)
    {
        String modelProperty = System.getProperty("polySolidModel");
        if ( modelProperty == null || modelProperty.isBlank() ) {
            return;
        }

        try {
            int modelId = Integer.parseInt(modelProperty);
            model.setSolidModelName(SolidModelNames.fromId(modelId));
            return;
        }
        catch ( NumberFormatException e ) {
            // Not a numeric id; continue with enum-name parsing.
        }

        try {
            model.setSolidModelName(SolidModelNames.valueOf(modelProperty));
        }
        catch ( IllegalArgumentException e ) {
            System.err.println("[PolyhedralBoundedSolidExample] Ignoring unknown "
                + "polySolidModel='" + modelProperty + "'");
        }
    }

    private static void applyOptionOverrides(
        DebuggerModel model,
        CommandLineOptions options)
    {
        RendererConfiguration quality = model.getQuality();

        if ( options.getSolidModelName() != null ) {
            model.setSolidModelName(options.getSolidModelName());
        }
        if ( options.getCsgSampleName() != null ) {
            model.setCsgSample(options.getCsgSampleName());
        }
        if ( options.getDrawPoints() != null ) {
            quality.setPoints(options.getDrawPoints());
        }
        if ( options.getDrawWires() != null ) {
            quality.setWires(options.getDrawWires());
        }
        if ( options.getDrawSurfaces() != null ) {
            quality.setSurfaces(options.getDrawSurfaces());
        }
        if ( options.getShadingType() != null ) {
            quality.setShadingType(options.getShadingType());
        }
        if ( options.getVertexNormalSmoothingThresholdDegrees() != null ) {
            quality.setVertexNormalSmoothingThresholdDegrees(
                options.getVertexNormalSmoothingThresholdDegrees().doubleValue());
        }
        if ( options.getEdgeIndex() != null ) {
            model.setEdgeIndex(options.getEdgeIndex().intValue());
        }
        if ( options.getKurlanderBowlMotifIndex() != null ) {
            model.setKurlanderBowlSingleMotifIndex(
                options.getKurlanderBowlMotifIndex().intValue());
        }
    }

    private static void runMotifSweep(DebuggerModel model, String outputPath)
    {
        int total = CsgKurlanderBowlFixture.getSingleMotifCount();
        int stars = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        String prefix = outputPath;
        int dotIndex = prefix.lastIndexOf('.');
        String stem = dotIndex < 0 ? prefix : prefix.substring(0, dotIndex);
        String ext = dotIndex < 0 ? ".png" : prefix.substring(dotIndex);
        model.setSolidModelName(SolidModelNames.CSG_DIRECT);
        model.setCsgSample(CsgSampleNames.KURLANDER_BOWL_SINGLE_MOTIF);
        int ok = 0;
        int empty = 0;
        int invalid = 0;
        int blackFaces = 0;
        int unchanged = 0;
        int exception = 0;
        for ( int motif = 0; motif < total; motif++ ) {
            String kind = motif < stars ? "STAR" : "MOON";
            int kindIndex = motif < stars ? motif : motif - stars;
            String tag = kind + "[" + kindIndex + "]";
            String filename = stem + "_" + String.format("%02d", motif) +
                "_" + kind + kindIndex + ext;
            int originalBowlFaces = -1;
            try {
                PolyhedralBoundedSolid[] preview =
                    CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(
                        motif);
                if ( preview != null && preview[0] != null ) {
                    originalBowlFaces = preview[0].getPolygonsList().size();
                }
            }
            catch ( Throwable t ) {
                /* fall through */
            }
            model.setKurlanderBowlSingleMotifIndex(motif);
            buildSolidWithRecovery(model);
            try {
                Jogl4HeadlessRenderer renderer = new Jogl4HeadlessRenderer(
                    model, new File(filename));
                renderer.render();
            }
            catch ( Throwable t ) {
                model.setErrorState("RenderException: " +
                    t.getClass().getSimpleName() + " - " + t.getMessage());
            }
            String status;
            String detail;
            if ( model.isErrorState() ) {
                status = "EXCEPTION";
                detail = " err=" + model.getErrorMessage();
                exception++;
            }
            else {
                PolyhedralBoundedSolid solid = model.getSolid();
                int faces = solid == null ? 0
                    : solid.getPolygonsList().size();
                if ( faces == 0 ) {
                    status = "EMPTY";
                    detail = "";
                    empty++;
                }
                else if ( faces == originalBowlFaces ) {
                    status = "UNCHANGED";
                    detail = " faces=" + faces;
                    unchanged++;
                }
                else {
                    boolean valid = false;
                    try {
                        valid = PolyhedralBoundedSolidValidationEngine
                            .validateIntermediate(solid);
                    }
                    catch ( Throwable t ) {
                        /* leave valid=false */
                    }
                    if ( !valid ) {
                        status = "INVALID";
                        detail = " faces=" + faces;
                        invalid++;
                    }
                    else {
                        StringBuilder orientationMsg = new StringBuilder();
                        boolean orientationOK = true;
                        try {
                            orientationOK =
                                PolyhedralBoundedSolidGeometricValidator
                                    .validateConsistentFaceOrientations(
                                        solid, orientationMsg);
                        }
                        catch ( Throwable t ) {
                            orientationOK = true;
                        }
                        if ( !orientationOK ) {
                            int firstLine =
                                orientationMsg.toString().indexOf('\n');
                            String preview = firstLine > 0
                                ? orientationMsg.substring(0, firstLine).trim()
                                : "(orientation flagged)";
                            status = "BLACK_FACES";
                            detail = " faces=" + faces +
                                " " + preview;
                            blackFaces++;
                        }
                        else {
                            status = "OK";
                            detail = " faces=" + faces +
                                " bowlFaces=" + originalBowlFaces;
                            ok++;
                        }
                    }
                }
            }
            System.out.println("[SWEEP-" + status + "] " + tag +
                " motif=" + motif + detail);
        }
        System.out.println("[SWEEP-SUMMARY] ok=" + ok + " empty=" + empty +
            " invalid=" + invalid + " blackFaces=" + blackFaces +
            " unchanged=" + unchanged +
            " exception=" + exception + " total=" + total);
    }

    public static void buildSolidWithRecovery(DebuggerModel model)
    {
        try {
            model.clearErrorState();
            model.setSolid(PolyhedralBoundedSolidModelingTools.buildSolid(model));
            if ( model.getSolid() == null ) {
                throw new IllegalStateException("Solid builder returned null");
            }
            model.clampFaceIndex();
        }
        catch ( Throwable e ) {
            model.setErrorState(formatBuildErrorMessage(model, e));
        }
    }

    private static String formatBuildErrorMessage(DebuggerModel model, Throwable e)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("Build error");
        if ( model.getSolidModelName() != null ) {
            msg.append(" [").append(model.getSolidModelName().name()).append("]");
        }
        msg.append(": ").append(e.getClass().getSimpleName());
        if ( e.getMessage() != null && !e.getMessage().isEmpty() ) {
            msg.append(" - ").append(e.getMessage());
        }
        return msg.toString();
    }

    private static void exportAppelDumpIfRequested(
        DebuggerModel model,
        CommandLineOptions options)
    {
        if ( options.getAppelDumpPath() == null ||
             options.getAppelDumpPath().isBlank() ||
             options.getEdgeIndex() == null ||
             options.getEdgeIndex().intValue() != -3 ||
             model.getSolid() == null ) {
            return;
        }

        HiddenLineRenderer.AppelAlgorithmDump dump =
            HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createDebugBodyScene(model), model.getCamera(),
                new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer());

        File dumpFile = new File(options.getAppelDumpPath());
        File parent = dumpFile.getParentFile();
        if ( parent != null && !parent.exists() ) {
            parent.mkdirs();
        }

        try {
            Files.writeString(dumpFile.toPath(),
                serializeAppelDump(model, dump), StandardCharsets.UTF_8);
            System.out.println("[PolyhedralBoundedSolidExample] Exported " +
                dumpFile.getPath());
        }
        catch ( IOException e ) {
            throw new IllegalStateException(
                "Cannot write appel dump to '" + dumpFile.getPath() + "'", e);
        }
    }

    private static List<SimpleBody> createDebugBodyScene(DebuggerModel model)
    {
        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>();
        SimpleBody body = new SimpleBody();
        Matrix4x4d modelMatrix = model.getSolidModelMatrix();
        body.setGeometry(model.getSolid());
        body.setPosition(new Vector3Dd());
        body.setRotation(modelMatrix);
        body.setRotationInverse(modelMatrix.inverse());
        bodies.add(body);
        return bodies;
    }

    private static String serializeAppelDump(
        DebuggerModel model,
        HiddenLineRenderer.AppelAlgorithmDump dump)
    {
        StringBuilder json = new StringBuilder(32768);
        json.append("{\n");
        json.append("  \"solidModel\": ");
        appendJsonString(json, model.getSolidModelName().name());
        json.append(",\n");
        json.append("  \"camera\": {\n");
        appendJsonVector(json, "position", model.getCamera().getPosition(), 4);
        json.append(",\n");
        appendJsonVector(json, "front", model.getCamera().getFront(), 4);
        json.append(",\n");
        appendJsonVector(json, "up", model.getCamera().getUp(), 4);
        json.append(",\n");
        appendJsonMatrix(json, "rotation", model.getCamera().getRotation(), 4);
        json.append("\n  },\n");
        json.append("  \"edgeIndex\": ").append(model.getEdgeIndex()).append(",\n");
        json.append("  \"edgeCount\": ").append(dump.edges.size()).append(",\n");
        json.append("  \"edges\": [\n");
        for ( int i = 0; i < dump.edges.size(); i++ ) {
            HiddenLineRenderer.AppelEdgeDump edge = dump.edges.get(i);
            json.append("    {\n");
            json.append("      \"edgeIndex\": ").append(edge.edgeIndex).append(",\n");
            json.append("      \"edgeType\": ");
            appendJsonString(json, edge.edgeTypeName);
            json.append(",\n");
            json.append("      \"faceIds\": [")
                .append(edge.face1Id).append(", ")
                .append(edge.face2Id).append("],\n");
            appendJsonVector(json, "start", edge.start, 6);
            json.append(",\n");
            appendJsonVector(json, "end", edge.end, 6);
            json.append(",\n");
            json.append("      \"initialQI\": ")
                .append(edge.initialQuantitativeInvisibility).append(",\n");
            json.append("      \"events\": [\n");
            for ( int j = 0; j < edge.events.size(); j++ ) {
                HiddenLineRenderer.AppelEventDump event = edge.events.get(j);
                json.append("        {\"t\": ")
                    .append(formatDouble(event.t))
                    .append(", \"deltaQI\": ")
                    .append(event.deltaQI)
                    .append(", \"contourEdgeIndex\": ")
                    .append(event.contourEdgeIndex)
                    .append(", \"visibleFaceId\": ")
                    .append(event.visibleFaceId)
                    .append("}");
                if ( j + 1 < edge.events.size() ) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("      ],\n");
            json.append("      \"segments\": [\n");
            for ( int j = 0; j < edge.segments.size(); j++ ) {
                HiddenLineRenderer.AppelSegmentDump segment = edge.segments.get(j);
                json.append("        {\n");
                json.append("          \"t\": [")
                    .append(formatDouble(segment.tStart)).append(", ")
                    .append(formatDouble(segment.tEnd)).append("],\n");
                appendJsonVector(json, "start", segment.start, 10);
                json.append(",\n");
                appendJsonVector(json, "end", segment.end, 10);
                json.append(",\n");
                appendJsonVector(json, "midpoint", segment.midpoint, 10);
                json.append(",\n");
                json.append("          \"midpointQI\": ")
                    .append(segment.midpointQuantitativeInvisibility).append(",\n");
                json.append("          \"midpointContributorFaceIds\": ");
                appendJsonIntArray(json, segment.midpointContributorFaceIds);
                json.append(",\n");
                json.append("          \"classification\": ");
                appendJsonString(json, segment.classification);
                json.append("\n        }");
                if ( j + 1 < edge.segments.size() ) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("      ]\n");
            json.append("    }");
            if ( i + 1 < dump.edges.size() ) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static void appendJsonString(StringBuilder json, String value)
    {
        json.append("\"")
            .append(value.replace("\\", "\\\\").replace("\"", "\\\""))
            .append("\"");
    }

    private static void appendJsonVector(
        StringBuilder json,
        String name,
        Vector3Dd vector,
        int indent)
    {
        indent(json, indent);
        json.append("\"").append(name).append("\": [")
            .append(formatDouble(vector.x())).append(", ")
            .append(formatDouble(vector.y())).append(", ")
            .append(formatDouble(vector.z())).append("]");
    }

    private static void appendJsonMatrix(
        StringBuilder json,
        String name,
        Matrix4x4d matrix,
        int indent)
    {
        indent(json, indent);
        json.append("\"").append(name).append("\": [");
        for ( int row = 0; row < 4; row++ ) {
            if ( row > 0 ) {
                json.append(", ");
            }
            json.append("[");
            for ( int col = 0; col < 4; col++ ) {
                if ( col > 0 ) {
                    json.append(", ");
                }
                json.append(formatDouble(matrix.get(row, col)));
            }
            json.append("]");
        }
        json.append("]");
    }

    private static void appendJsonIntArray(
        StringBuilder json,
        List<Integer> values)
    {
        json.append("[");
        for ( int i = 0; i < values.size(); i++ ) {
            if ( i > 0 ) {
                json.append(", ");
            }
            json.append(values.get(i).intValue());
        }
        json.append("]");
    }

    private static String formatDouble(double value)
    {
        return Double.toString(value);
    }

    private static void indent(StringBuilder json, int spaces)
    {
        for ( int i = 0; i < spaces; i++ ) {
            json.append(' ');
        }
    }
}
