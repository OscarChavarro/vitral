import models.DebuggerModel;
import models.SolidModelNames;
import options.CommandLineOptions;
import render.Jogl4HeadlessRenderer;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.material.RendererConfiguration;

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
            System.err.println("Usage: [--offline] [--output <file.png>]");
            return;
        }

        // Keep the debugger process alive and surface fatal kernel issues as exceptions.
        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);

        configureInitialModelFromSystemProperty(model);
        applyOptionOverrides(model, options);
        buildSolidWithRecovery(model);

        if ( options.isOffline() ) {
            Jogl4HeadlessRenderer renderer = new Jogl4HeadlessRenderer(
                model, new File(options.getOutputPath()));
            renderer.render();
            return;
        }

        InteractiveDebugger.launch(model);
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
