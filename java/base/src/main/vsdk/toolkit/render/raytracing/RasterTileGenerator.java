package vsdk.toolkit.render.raytracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vsdk.toolkit.media.Image;

/**
Generates image sub-regions ("tiles") according to a scheduling strategy.

Current strategies:
  - `LINEAR`: creates horizontal bands.
  - `SERIAL`: creates one tile for the whole requested region.
*/
public class RasterTileGenerator
{
    private final RasterTileGenerationStrategy strategy;
    private final Image image;
    private final int startX;
    private final int startY;
    private final int width;
    private final int height;
    private final int numberOfThreads;
    private final List<RasterTileArea> tiles;

    public RasterTileGenerator(RasterTileGenerationStrategy strategy,
                               Image image,
                               int width,
                               int height,
                               int numberOfThreads)
    {
        this(strategy, image, 0, 0, width, height, numberOfThreads);
    }

    public RasterTileGenerator(RasterTileGenerationStrategy strategy,
                               Image image,
                               int startX,
                               int startY,
                               int width,
                               int height,
                               int numberOfThreads)
    {
        if ( strategy == null ) {
            throw new IllegalArgumentException("strategy can not be null");
        }
        if ( image == null ) {
            throw new IllegalArgumentException("image can not be null");
        }
        if ( startX < 0 || startY < 0 ) {
            throw new IllegalArgumentException("origin must be >= 0");
        }
        if ( width <= 0 ) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if ( height <= 0 ) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if ( startX + width > image.getXSize() || startY + height > image.getYSize() ) {
            throw new IllegalArgumentException(
                "requested tile area must be inside image");
        }
        if ( numberOfThreads <= 0 ) {
            throw new IllegalArgumentException("numberOfThreads must be > 0");
        }

        this.strategy = strategy;
        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.numberOfThreads = numberOfThreads;
        this.tiles = Collections.unmodifiableList(generateTiles());
    }

    public List<RasterTileArea> getTiles()
    {
        return tiles;
    }

    private List<RasterTileArea> generateTiles()
    {
        switch ( strategy ) {
          case LINEAR:
            return generateLinearTiles();
          case SERIAL:
            return generateSerialTile();
          default:
            throw new IllegalStateException("Unsupported tile strategy: " + strategy);
        }
    }

    private List<RasterTileArea> generateLinearTiles()
    {
        ArrayList<RasterTileArea> out = new ArrayList<RasterTileArea>();

        int workerBands = numberOfThreads;
        if ( workerBands > height ) {
            workerBands = height;
        }

        int baseBandHeight = height / workerBands;
        int extraRows = height % workerBands;
        int y = 0;

        for ( int i = 0; i < workerBands; i++ ) {
            int currentBandHeight = baseBandHeight + (i < extraRows ? 1 : 0);
            out.add(new RasterTileArea(
                image, startX, startY + y, width, currentBandHeight));
            y += currentBandHeight;
        }

        return out;
    }

    private List<RasterTileArea> generateSerialTile()
    {
        ArrayList<RasterTileArea> out = new ArrayList<RasterTileArea>(1);
        out.add(new RasterTileArea(image, startX, startY, width, height));
        return out;
    }
}
