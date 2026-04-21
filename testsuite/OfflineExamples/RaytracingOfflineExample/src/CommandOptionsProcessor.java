final class CommandOptionsProcessor {
    private static final String DEFAULT_SCENE_FILE =
        "../../../etc/geometry/mitscenes/balls.ray";
    private static final String DEFAULT_OUTPUT_FILE_NAME = "./output.ppm";

    private String sceneFile = DEFAULT_SCENE_FILE;
    private String outputFile = DEFAULT_OUTPUT_FILE_NAME;
    private boolean save = true;
    private boolean parallel = false;
    private boolean showHelp = false;

    private CommandOptionsProcessor()
    {
    }

    static CommandOptionsProcessor process(String[] args)
    {
        CommandOptionsProcessor options = new CommandOptionsProcessor();
        int positionalCount = 0;

        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            if ( "nosave".equals(arg) || "--nosave".equals(arg) || "-n".equals(arg) ) {
                options.save = false;
                continue;
            }
            if ( "-parallel".equals(arg) || "--parallel".equals(arg) ) {
                options.parallel = true;
                continue;
            }
            if ( "--help".equals(arg) || "-h".equals(arg) ) {
                options.showHelp = true;
                continue;
            }
            if ( "--scene".equals(arg) || "-s".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    System.err.println("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.sceneFile = args[++i];
                continue;
            }
            if ( "--output".equals(arg) || "-o".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    System.err.println("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.outputFile = args[++i];
                continue;
            }
            if ( arg.startsWith("-") ) {
                System.err.println("Unknown option: " + arg);
                options.showHelp = true;
                return options;
            }

            if ( positionalCount == 0 ) {
                options.sceneFile = arg;
            }
            else if ( positionalCount == 1 ) {
                options.outputFile = arg;
            }
            else {
                System.err.println("Unexpected argument: " + arg);
                options.showHelp = true;
                return options;
            }
            positionalCount++;
        }

        return options;
    }

    static void printUsage()
    {
        System.out.println("Usage: RaytracerSimple [options] [scene_file]");
        System.out.println("Options:");
        System.out.println("  --scene, -s <file>     MIT scene file (.ray)");
        System.out.println("  --output, -o <file>    Output image file (.ppm/.png/.jpg)");
        System.out.println("  --nosave, -n           Render only, no image file");
        System.out.println("  -parallel, --parallel  Render tiles in parallel");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Legacy compatibility:");
        System.out.println("  - `nosave` (without dashes) is still accepted.");
    }

    String getSceneFile()
    {
        return sceneFile;
    }

    String getOutputFile()
    {
        return outputFile;
    }

    boolean shouldSave()
    {
        return save;
    }

    boolean shouldUseParallelExecutor()
    {
        return parallel;
    }

    boolean shouldShowHelp()
    {
        return showHelp;
    }
}
