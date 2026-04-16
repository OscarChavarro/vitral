public class TextEraserRunnable implements Runnable
{
    private CommandServer parent;

    public TextEraserRunnable(CommandServer parent)
    {
        this.parent = parent;
    }

    public void run()
    {
        while ( true ) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - parent.lastTimeTyped;

            if ( deltaTime > 50 ) {
                if ( parent.jtf != null &&
                     parent.jtf.getDocument().getLength() > 0 ) {
                    parent.notifySpeech();
                }
            }

            try {
                Thread.sleep(500);
            }
            catch ( Exception e) {
                System.err.println("Error sleeping thread");
            }
        }
    }
}
