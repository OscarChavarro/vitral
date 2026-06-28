package vsdk.toolkit.common.statistics;

import java.util.List;

public final class SolidTextureStatistics
{
    public long callsToNoise;
    public long callsToDNoise;

    public SolidTextureStatistics()
    {
        reset();
    }

    public SolidTextureStatistics(List<SolidTextureStatistics> partsPerThread)
    {
        reset();
        if ( partsPerThread == null ) {
            return;
        }
        for ( SolidTextureStatistics part : partsPerThread ) {
            if ( part != null ) {
                callsToNoise += part.callsToNoise;
                callsToDNoise += part.callsToDNoise;
            }
        }
    }

    public void reset()
    {
        callsToNoise = 0L;
        callsToDNoise = 0L;
    }
}
