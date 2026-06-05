import java.io.File;

import models.TangibleInterfaceGizmosModel;
import options.CommandLineOptions;
import render.Jogl4HeadlessRenderer;
import vsdk.toolkit.common.VSDK;

public class TangibleInterfaceGizmoCreator
{
    public static void main(String[] args)
    {
        TangibleInterfaceGizmosModel model = new TangibleInterfaceGizmosModel();
        CommandLineOptions options;

        try {
            options = CommandLineOptions.parse(args);
        }
        catch ( IllegalArgumentException e ) {
            System.err.println("[TangibleInterfaceGizmoCreator] " + e.getMessage());
            System.err.println(
                "Usage: [--offline] [--screenshot <file.png>] [--solidModel <cube|sphere>]");
            return;
        }

        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);

        applyOptionOverrides(model, options);
        buildSolidWithRecovery(model);

        if ( options.isOffline() ) {
            Jogl4HeadlessRenderer renderer = new Jogl4HeadlessRenderer(
                model, new File(options.getOutputPath()));
            renderer.render();
            if ( model.isErrorState() ) {
                System.err.println("[TangibleInterfaceGizmoCreator] BUILD-ERROR: "
                    + model.getErrorMessage());
                System.exit(2);
            }
            return;
        }

        InteractiveModelPreviewer.launch(model);
    }

    private static void applyOptionOverrides(
        TangibleInterfaceGizmosModel model,
        CommandLineOptions options)
    {
        if ( options.getSolidModelName() != null ) {
            model.setSolidModelName(options.getSolidModelName());
        }
        if ( options.getDrawPoints() != null ) {
            model.getQuality().setPoints(options.getDrawPoints());
        }
        if ( options.getDrawWires() != null ) {
            model.getQuality().setWires(options.getDrawWires());
        }
        if ( options.getDrawSurfaces() != null ) {
            model.getQuality().setSurfaces(options.getDrawSurfaces());
        }
        if ( options.getShadingType() != null ) {
            model.getQuality().setShadingType(options.getShadingType());
        }
    }

    public static void buildSolidWithRecovery(TangibleInterfaceGizmosModel model)
    {
        try {
            model.clearErrorState();
            model.setSolid(models.GeneralModelsBuilder.buildSolid(model));
            if ( model.getSolid() == null ) {
                throw new IllegalStateException("Solid builder returned null");
            }
        }
        catch ( Throwable e ) {
            model.setErrorState(formatBuildErrorMessage(model, e));
        }
    }

    private static String formatBuildErrorMessage(
        TangibleInterfaceGizmosModel model,
        Throwable e)
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
