package application;

public class MainThread implements Runnable
{
    private final String[] args;
    public MainThread(String[] args)
    {
        this.args = args;
    }

    @Override
    public void run()
    {
        new SceneEditorApplication(args);
    }
}
