public class Animation
{
    public static final int FRAMES_PER_SECOND = 30;
    public static final int FRAME_DELAY_MILLIS = 1000 / FRAMES_PER_SECOND;

    private static final double FULL_ROTATION_RADIANS = 2.0 * Math.PI;
    private static final double ROTATION_PERIOD_SECONDS = 8.0;
    private static final double ANGULAR_SPEED_RAD_PER_SECOND =
        FULL_ROTATION_RADIANS / ROTATION_PERIOD_SECONDS;

    private long lastTickNanos = -1L;

    public void reset()
    {
        lastTickNanos = -1L;
    }

    public void tick(ShadersModel model)
    {
        if ( model == null || !model.isAnimationEnabled() ) {
            reset();
            return;
        }

        long now = System.nanoTime();
        if ( lastTickNanos < 0L ) {
            lastTickNanos = now;
            return;
        }

        double elapsedSeconds = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;

        if ( elapsedSeconds < 0.0 ) {
            return;
        }
        if ( elapsedSeconds > 0.25 ) {
            elapsedSeconds = 0.25;
        }

        model.advanceSphereRotationRadians(ANGULAR_SPEED_RAD_PER_SECOND * elapsedSeconds);
    }
}
