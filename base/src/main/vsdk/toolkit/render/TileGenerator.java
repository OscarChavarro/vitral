package vsdk.toolkit.render;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
Generates image sub-regions ("tiles") according to a scheduling strategy.

Current `LINEAR` strategy creates horizontal bands covering the whole image.
Each returned rectangle follows Java's `<x, y, width, height>` convention,
so it can be mapped to raytracer limits as:
  - `limx1 = rect.x`
  - `limy1 = rect.y`
  - `limx2 = rect.x + rect.width`
  - `limy2 = rect.y + rect.height`
*/
public class TileGenerator
{
    private final TileGenerationStrategy strategy;
    private final int width;
    private final int height;
    private final int numberOfThreads;
    private final List<Rectangle> tiles;

    public TileGenerator(TileGenerationStrategy strategy,
                         int width,
                         int height,
                         int numberOfThreads)
    {
        if ( strategy == null ) {
            throw new IllegalArgumentException("strategy can not be null");
        }
        if ( width <= 0 ) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if ( height <= 0 ) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if ( numberOfThreads <= 0 ) {
            throw new IllegalArgumentException("numberOfThreads must be > 0");
        }

        this.strategy = strategy;
        this.width = width;
        this.height = height;
        this.numberOfThreads = numberOfThreads;
        this.tiles = Collections.unmodifiableList(generateTiles());
    }

    public List<Rectangle> getTiles()
    {
        return tiles;
    }

    private List<Rectangle> generateTiles()
    {
        switch ( strategy ) {
          case LINEAR:
            return generateLinearTiles();
          default:
            throw new IllegalStateException("Unsupported tile strategy: " + strategy);
        }
    }

    private List<Rectangle> generateLinearTiles()
    {
        ArrayList<Rectangle> out = new ArrayList<Rectangle>();

        int workerBands = numberOfThreads;
        if ( workerBands > height ) {
            workerBands = height;
        }

        int baseBandHeight = height / workerBands;
        int extraRows = height % workerBands;
        int y = 0;

        for ( int i = 0; i < workerBands; i++ ) {
            int currentBandHeight = baseBandHeight + (i < extraRows ? 1 : 0);
            out.add(new Rectangle(0, y, width, currentBandHeight));
            y += currentBandHeight;
        }

        return out;
    }
}
