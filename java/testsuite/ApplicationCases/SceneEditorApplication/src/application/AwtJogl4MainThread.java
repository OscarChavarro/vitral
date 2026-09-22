package application;

public class AwtJogl4MainThread implements Runnable
{
    private final String[] args;
    public AwtJogl4MainThread(String[] args)
    {
        this.args = args;
    }

    @Override
    public void run()
    {
        new AwtJogl4SceneEditorApplication(args);
    }
}
