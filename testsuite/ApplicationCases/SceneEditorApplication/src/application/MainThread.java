//===========================================================================

package application;

public class MainThread implements Runnable
{
    private String args[];
    public MainThread(String args[])
    {
        this.args = args;
    }
    public void run()
    {
        SceneEditorApplication app;
        app = new SceneEditorApplication(args);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
