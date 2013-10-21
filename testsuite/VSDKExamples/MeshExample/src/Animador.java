public class Animador implements Runnable
{
    public double t;
    public MeshExample bicho;

    public Animador(MeshExample padre)
    {
	bicho = padre;
    }

    public void run() {
        t = 0.0;
        try {
            while ( true ) {
                t = t + 0.1;
                System.out.println("T = 1" + t);
                bicho.x = t;
                bicho.canvas.repaint();
                Thread.sleep(1000/24);
                if ( t > 5.0 ) {
                    break;
		}
	    }
	}
	catch ( Exception e ) {
	}
    }
}
