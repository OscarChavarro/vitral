package application;

public class MainThread implements Runnable
{
    private String args[];
    public MainThread(String args[])
    {
        this.args = args;
    }

    @Override
    public void run()
    {
        SceneEditorApplication app;
        app = new SceneEditorApplication(args);
    }
}
