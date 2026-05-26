import models.DebuggerModel;
import models.SolidModelNames;
import models.CsgSampleNames;
import gui.CameraFaceFocusInteraction;
import options.CommandLineOptions;
import render.Jogl4HeadlessRenderer;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture;

import java.io.File;

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
                "Usage: [--offline] [--screenshot <file.png>] [--faceId <id>]");
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
            Jogl4HeadlessRenderer renderer = new Jogl4HeadlessRenderer(
                model, new File(options.getOutputPath()));
            renderer.render();
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
}
