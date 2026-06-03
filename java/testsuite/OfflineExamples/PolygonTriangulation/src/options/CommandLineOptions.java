package options;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.PolygonModel;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;

public class CommandLineOptions
{
    private static final Pattern INPUT_FILE_PATTERN =
        Pattern.compile("^example(\\d+)\\.polygon$");
    private static final String DEFAULT_INPUT_FILE =
        "../../../../etc/polygons/example01.polygon";
    private static final int DEFAULT_ZONE_WIDTH = 512;
    private static final int DEFAULT_ZONE_HEIGHT = 512;

    private String inputFileName;
    private String outputFileName;
    private int zoneWidth;
    private int zoneHeight;
    private boolean showHelp;

    public CommandLineOptions()
    {
        inputFileName = DEFAULT_INPUT_FILE;
        outputFileName = null;
        zoneWidth = DEFAULT_ZONE_WIDTH;
        zoneHeight = DEFAULT_ZONE_HEIGHT;
        showHelp = false;
    }

    public static CommandLineOptions parse(String[] args)
    {
        CommandLineOptions options = new CommandLineOptions();
        int positionalIndex = 0;

        for ( int i = 0; args != null && i < args.length; i++ ) {
            String argument = args[i];

            if ( "--help".equals(argument) || "-h".equals(argument) ) {
                options.showHelp = true;
                continue;
            }

            if ( "--input".equals(argument) || "-i".equals(argument) ) {
                options.inputFileName = requireValue(args, ++i, argument);
                continue;
            }

            if ( "--output".equals(argument) || "-o".equals(argument) ) {
                options.outputFileName = requireValue(args, ++i, argument);
                continue;
            }

            if ( "--zone-width".equals(argument) ) {
                options.zoneWidth = parsePositiveInt(requireValue(args, ++i,
                    argument), argument);
                continue;
            }

            if ( "--zone-height".equals(argument) ) {
                options.zoneHeight = parsePositiveInt(requireValue(args, ++i,
                    argument), argument);
                continue;
            }

            if ( argument.startsWith("-") ) {
                throw new IllegalArgumentException("Unknown option: " + argument);
            }

            if ( positionalIndex == 0 ) {
                options.inputFileName = argument;
            }
            else if ( positionalIndex == 1 ) {
                options.outputFileName = argument;
            }
            else {
                throw new IllegalArgumentException(
                    "Unexpected positional argument: " + argument);
            }
            positionalIndex++;
        }

        if ( options.outputFileName == null || options.outputFileName.isBlank() ) {
            options.outputFileName = deriveOutputFileName(options.inputFileName);
        }

        return options;
    }

    public static void printUsage()
    {
        System.out.println("Usage: PolygonTriangulation [options] [input_file] [output_file]");
        System.out.println("Options:");
        System.out.println("  --input, -i <file>       Polygon input file (.polygon)");
        System.out.println("  --output, -o <file>      PNG output file");
        System.out.println("  --zone-width <pixels>    Width of each image zone (default 512)");
        System.out.println("  --zone-height <pixels>   Height of each image zone (default 512)");
        System.out.println("  --help, -h               Show this help");
    }

    public PolygonModel toPolygonModel(Polygon2D polygon2D)
    {
        return new PolygonModel(polygon2D, inputFileName, outputFileName,
            zoneWidth, zoneHeight);
    }

    public String getInputFileName()
    {
        return inputFileName;
    }

    public String getOutputFileName()
    {
        return outputFileName;
    }

    public int getZoneWidth()
    {
        return zoneWidth;
    }

    public int getZoneHeight()
    {
        return zoneHeight;
    }

    public boolean shouldShowHelp()
    {
        return showHelp;
    }

    private static String requireValue(String[] args, int index, String option)
    {
        if ( args == null || index >= args.length ) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static int parsePositiveInt(String value, String option)
    {
        int parsedValue = Integer.parseInt(value);
        if ( parsedValue <= 0 ) {
            throw new IllegalArgumentException(
                option + " must be a positive integer: " + value);
        }
        return parsedValue;
    }

    private static String deriveOutputFileName(String inputFileName)
    {
        String inputBaseName = Path.of(inputFileName).getFileName().toString();
        Matcher matcher = INPUT_FILE_PATTERN.matcher(inputBaseName);
        if ( matcher.matches() ) {
            return "output" + matcher.group(1) + ".png";
        }
        return "output.png";
    }
}
