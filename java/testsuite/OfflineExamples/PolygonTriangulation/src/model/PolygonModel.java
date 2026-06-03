package model;

import java.util.Objects;

import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;

public class PolygonModel
{
    private final Polygon2D polygon2D;
    private final String inputFileName;
    private final String outputFileName;
    private final int zoneWidth;
    private final int zoneHeight;

    public PolygonModel(Polygon2D polygon2D, String inputFileName,
            String outputFileName, int zoneWidth, int zoneHeight)
    {
        this.polygon2D = Objects.requireNonNull(polygon2D, "polygon2D");
        this.inputFileName = Objects.requireNonNull(inputFileName, "inputFileName");
        this.outputFileName = Objects.requireNonNull(outputFileName, "outputFileName");
        this.zoneWidth = zoneWidth;
        this.zoneHeight = zoneHeight;
    }

    public Polygon2D getPolygon2D()
    {
        return polygon2D;
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

    public int getImageWidth()
    {
        return zoneWidth * 2;
    }

    public int getImageHeight()
    {
        return zoneHeight;
    }
}
