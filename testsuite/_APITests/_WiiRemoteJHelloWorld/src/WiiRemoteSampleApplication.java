//===========================================================================
//= Example based on WiiRemoteJ.jar example "WRLImpl.java", by              =
//= Michael Diamond (version 1/05/07) posted on june 26/2007 at             =
//= http://www.wiili.org/forum/wiiremotej-v10-released-t2357.html           =
//= Modified by Oscar Xavier Chavarro - October 9 / 2007.                   =
//===========================================================================

// Java basic classes
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;

// Java GUI classes
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;

// Java multimedia classes
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;

// WiiRemoteJ classes
import wiiremotej.AccelerometerMouse;
import wiiremotej.AnalogStickMouse;
import wiiremotej.AbsoluteAnalogStickMouse;
import wiiremotej.ButtonMap;
import wiiremotej.ButtonMouseMap;
import wiiremotej.ButtonMouseWheelMap;
import wiiremotej.IRAccelerometerMouse;
import wiiremotej.IRMouse;
import wiiremotej.MotionAccelerometerMouse;
import wiiremotej.PrebufferedSound;
import wiiremotej.RelativeAnalogStickMouse;
import wiiremotej.TiltAccelerometerMouse;
import wiiremotej.WiiRemote;
import wiiremotej.WiiRemoteExtension;
import wiiremotej.WiiRemoteJ;
import wiiremotej.event.WiiRemoteAdapter;
import wiiremotej.event.WiiRemoteDiscoveredEvent;
import wiiremotej.event.WiiRemoteDiscoveryListener;
import wiiremotej.event.WRAccelerationEvent;
import wiiremotej.event.WRButtonEvent;
import wiiremotej.event.WRClassicControllerExtensionEvent;
import wiiremotej.event.WRExtensionEvent;
import wiiremotej.event.WRIREvent;
import wiiremotej.event.WRNunchukExtensionEvent;
import wiiremotej.event.WRStatusEvent;

/**
Implements WiiRemoteListener and acts as a general test class. Note that you
can ignore the main method pretty much, as it mostly has to do with the graphs
and GUIs.
At the very end though, there's an example of how to connect to a remote and
how to prebuffer audio files.
*/

class MyFileThread implements Runnable
{
    private WiiRemoteSampleApplication parent;

    public MyFileThread(WiiRemoteSampleApplication parent)
    {
        this.parent = parent;
    }

    public void run()
    {
        try {
            File dataF = new File("data.dat");
            byte dataO[] = null;
            if ( dataF.exists() ) {
                dataO = new byte[0x0040];
                DataInputStream dataS = new DataInputStream(new FileInputStream(dataF));
                dataS.readFully(dataO);
                dataS.close();
            }
                                
            File data2F = new File("data2.dat");
            byte data2O[] = null;
            if ( data2F.exists() ) {
                data2O = new byte[0xFFFF];
                DataInputStream data2S = new DataInputStream(new FileInputStream(data2F));
                data2S.readFully(data2O);
                data2S.close();
            }
                                
            System.out.println("Searching address...");
            //byte[] address = new byte[]{0x00, 0x17, (byte)0xAB};
            //byte[] address = new byte[]{0x0F, 0x04, 0x00, 0x01, 0x01, 0x04};
                                
            /**/
            byte[] data = parent.remote.readData(new byte[]{0x00, 0x00, 0x00, 0x00}, 0x0040);
            System.out.println("Read complete (data)");
            if ( !dataF.exists() ) {
                DataOutputStream dataOS = new DataOutputStream(new FileOutputStream(dataF));
                dataOS.write(data, 0, data.length);
                dataOS.close();
            }
            else {
                System.out.println("Comparing arrs (data)");
                for ( int c = 0; c < data.length; c++ ) {
                    //System.out.println("0x" + Integer.toHexString(data[c]) + " : 0x" + Integer.toHexString(dataO[c]));
                    if (data[c] != dataO[c])System.out.println("Flash: 0x" + Integer.toHexString(c));
                }
                System.out.println("Comparing complete");
            }
            /**/                                
                                
            /*
            byte[] data2 = remote.readData(new byte[]{0x04, (byte)0xA2, 0x00, 0x00}, 65535);
            System.out.println("Read complete (data2)");
            if ( !data2F.exists() ) {
                DataOutputStream data2OS = new DataOutputStream(new FileOutputStream(data2F));
                data2OS.write(data2, 0, data2.length);
                data2OS.close();
            }
            else {
                System.out.println("Comparing arrs (data2)");
                for ( int c = 0; c < data2.length; c++ ) {
                    System.out.println("0x" + Integer.toHexString(data2[c]) + " : 0x" + Integer.toHexString(data2O[c]));
                    if (data2[c] != data2O[c])System.out.println("Register: 0x" + Integer.toHexString(c + 0x04A20000));
                }
                System.out.println("Comparing complete");
            }
            */
                                
            System.out.println("Search complete.");
        }
        catch (Exception e){e.printStackTrace();}
    }
}

class MyGraphPanel extends JPanel
{
    private WiiRemoteSampleApplication parent;

    public MyGraphPanel(WiiRemoteSampleApplication parent)
    {
        this.parent = parent;
    }

    public void paintComponent(Graphics graphics)
    {
        if ( parent.t >= parent.viewportXSize || parent.accelerometerSource != parent.lastSource ) {
            parent.t = 0;
            parent.lastSource = parent.accelerometerSource;
            graphics.clearRect(0, 0, parent.viewportXSize, 600);
            graphics.fillRect(0, 0, parent.viewportXSize, 600);
            graphics.setColor(Color.WHITE);
            graphics.drawLine(0, 300, parent.viewportXSize, 300);
        }

        graphics.setColor(Color.RED);
        graphics.drawLine(parent.t, parent.lastX, parent.t, parent.x);
        graphics.setColor(Color.GREEN);
        graphics.drawLine(parent.t, parent.lastY, parent.t, parent.y);
        graphics.setColor(Color.BLUE);
        graphics.drawLine(parent.t, parent.lastZ, parent.t, parent.z);
    }
}

class MyWiiRemoteDiscoveryListener implements WiiRemoteDiscoveryListener
{
    public void wiiRemoteDiscovered(WiiRemoteDiscoveredEvent evt)
    {
        evt.getWiiRemote().addWiiRemoteListener(new WiiRemoteSampleApplication(evt.getWiiRemote()));
    }
                
    public void findFinished(int numberFound)
    {
        System.out.println("Found " + numberFound + " remotes!");
    }
}

class MyMouseTestPanel extends JPanel
{
    private int LS;
    private WiiRemoteSampleApplication parent;

    public MyMouseTestPanel(int LS, WiiRemoteSampleApplication parent)
    {
        this.LS = LS;
        this.parent = parent;
    }

    public void paintComponent(Graphics graphics)
    {
        graphics.clearRect(0, 0, 4*LS, 7*LS);
        graphics.setColor(Color.YELLOW);
        if ( parent.status == 0 ) {
            graphics.fillRect(parent.status*LS, (parent.accelerometerStatus+1)*LS, LS, LS);
        }
        else if ( parent.status == 3 ) {
            graphics.fillRect(parent.status*LS, (parent.analogStickStatus+1)*LS, LS, LS);
        }
        else {
            graphics.fillRect(parent.status*LS, LS, LS, LS);
        }
                    
        graphics.setColor(Color.BLACK);
        graphics.drawString("WM", (int)(LS*0.5), (int)(LS*1.5));
        graphics.drawString("WT", (int)(LS*0.5), (int)(LS*2.5));
        graphics.drawString("NM", (int)(LS*0.5), (int)(LS*3.5));
        graphics.drawString("NT", (int)(LS*0.5), (int)(LS*4.5));
        graphics.drawString("**", (int)(LS*1.5), (int)(LS*1.5));
        graphics.drawString("**", (int)(LS*2.5), (int)(LS*1.5));
        graphics.drawString("NA", (int)(LS*3.5), (int)(LS*1.5));
        graphics.drawString("NR", (int)(LS*3.5), (int)(LS*2.5));
        graphics.drawString("LA", (int)(LS*3.5), (int)(LS*3.5));
        graphics.drawString("LR", (int)(LS*3.5), (int)(LS*4.5));
        graphics.drawString("RA", (int)(LS*3.5), (int)(LS*5.5));
        graphics.drawString("RR", (int)(LS*3.5), (int)(LS*6.5));
                    
        paintChildren(graphics);
    }
}

public class WiiRemoteSampleApplication extends WiiRemoteAdapter
{
    //true = wii remote, false = nunchuk
    public static boolean accelerometerSource = true;
    public static boolean lastSource = true;
    
    private static boolean mouseTestingOn;

    public int status = 0;
    public static int accelerometerStatus = 0;
    public static int analogStickStatus = 0;

    private static JFrame mouseTestFrame;
    private static JPanel mouseTestPanel;
    
    public WiiRemote remote;
    private static JFrame graphFrame;
    private static JPanel graph;
    private static int[][] pixels;
    public static int t = 0;
    public static int x = 0;
    public static int y = 0;
    public static int z = 0;
    
    public static int lastX = 0;
    public static int lastY = 0;
    public static int lastZ = 0;

    public static int viewportXSize = 1600;
    
    private static PrebufferedSound prebuf;
    

    public WiiRemoteSampleApplication()
    {
        ;
    }

    public void run()
    {
        // Console logging options for WiiRemoteJ
        //WiiRemoteJ.setConsoleLoggingAll();
        WiiRemoteJ.setConsoleLoggingOff();

        try {
            //-----------------------------------------------------------------
            WiiRemoteDiscoveryListener listener;
            listener = new MyWiiRemoteDiscoveryListener();
            
            // Find and connect to a Wii Remote
            WiiRemote remote;

            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Looking for a Nintendo Wii Remote ... ");
            remote = WiiRemoteJ.findRemote();

            if ( remote == null ) {
                System.err.println("Nintendo Wii Remote controller not found. Exiting.");
                System.exit(1);
            }
            System.out.println("Ok!");
            System.out.println("---------------------------------------------------------------------------");

            remote.addWiiRemoteListener(new WiiRemoteSampleApplication(remote));
            remote.setAccelerometerEnabled(true);
            remote.setSpeakerEnabled(true);
            remote.setIRSensorEnabled(true, WRIREvent.BASIC);
            remote.setLEDIlluminated(0, true);

            ButtonMap bm;
            int bmData[] = new int[]{java.awt.event.KeyEvent.VK_CONTROL};

            bm = new ButtonMap(WRButtonEvent.HOME, ButtonMap.NUNCHUK,
                               WRNunchukExtensionEvent.C, bmData,
                               java.awt.event.InputEvent.BUTTON1_MASK, 0, -1);
            remote.getButtonMaps().add(bm);
            
            // Prebuffer a preformatted audio file
            System.out.print("Buffering audio file... ");
            long time = System.currentTimeMillis();
            AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.File("./etc/audioSample.au"));
            prebuf = WiiRemote.bufferSound(audio);
            time = System.currentTimeMillis()-time;
            time /= 1000;
            System.out.println(" done in " + time + " seconds.");

            //-----------------------------------------------------------------
            int LS = 50; //line spacing

            mouseTestFrame = new JFrame();
            mouseTestFrame.setTitle("Mouse test");
            mouseTestFrame.setSize(4*LS, 7*LS);
            mouseTestFrame.setResizable(false);
            
            mouseTestPanel = new MyMouseTestPanel(LS, this);
            
            mouseTestPanel.setLayout(new FlowLayout());
            mouseTestPanel.add(new JLabel("A          I       IA         AS"));
            mouseTestFrame.add(mouseTestPanel);
            
            graphFrame = new JFrame();
            graphFrame.setTitle("VITRAL concept test - Nintendo Wii Remote Accelerometer sensing");
            graphFrame.setSize(viewportXSize, 600);
            graphFrame.setResizable(true);
            graphFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            t = viewportXSize+1;
            pixels = new int[viewportXSize][600];
            graph = new MyGraphPanel(this);
            graphFrame.add(graph);
            graphFrame.setVisible(true);
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        WiiRemoteSampleApplication instance = new WiiRemoteSampleApplication();
        instance.run();
    }
    
    public WiiRemoteSampleApplication(WiiRemote remote)
    {
        this.remote = remote;
    }
    
    public void disconnected()
    {
        System.out.println("Remote disconnected... Please Wii again.");
        System.exit(0);
    }
    
    public void statusReported(WRStatusEvent evt)
    {
        System.out.println("Battery level: " + (double)evt.getBatteryLevel()/2+ "%");
        System.out.println("Continuous: " + evt.isContinuousEnabled());
        System.out.println("Remote continuous: " + remote.isContinuousEnabled());
    }
    
    public void IRInputReceived(WRIREvent evt)
    {
        /*
        for ( IRLight light : evt.getIRLights() ) {
            if ( light != null ) {
                System.out.println("X: "+light.getX());
                System.out.println("Y: "+light.getY());
            }
        }
        */
    }
    
    public void accelerationInputReceived(WRAccelerationEvent evt)
    {
        if ( accelerometerSource ) {
            lastX = x;
            lastY = y;
            lastZ = z;
            
            x = (int)(evt.getXAcceleration()/5*300)+300;
            y = (int)(evt.getYAcceleration()/5*300)+300;
            z = (int)(evt.getZAcceleration()/5*300)+300;
            
            t++;
            
            graph.repaint();
        }
        
        /*
        System.out.println("---Acceleration Data---");
        System.out.println("X: " + evt.getXAcceleration());
        System.out.println("Y: " + evt.getYAcceleration());
        System.out.println("Z: " + evt.getZAcceleration());
        */
    }
    
    public void extensionInputReceived(WRExtensionEvent evt)
    {
        if ( evt instanceof WRNunchukExtensionEvent ) {
            WRNunchukExtensionEvent NEvt = (WRNunchukExtensionEvent)evt;
            
            if ( !accelerometerSource ) {
                WRAccelerationEvent AEvt = NEvt.getAcceleration();
                lastX = x;
                lastY = y;
                lastZ = z;
                
                x = (int)(AEvt.getXAcceleration()/5*300)+300;
                y = (int)(AEvt.getYAcceleration()/5*300)+300;
                z = (int)(AEvt.getZAcceleration()/5*300)+300;
                
                t++;
                
                graph.repaint();
            }
            
            if ( NEvt.wasPressed(WRNunchukExtensionEvent.C) ) {
                System.out.println("Jump...");
            }
            if ( NEvt.wasPressed(WRNunchukExtensionEvent.Z) ) {
                System.out.println("And crouch.");
            }
        }
        else if ( evt instanceof WRClassicControllerExtensionEvent ) {
            WRClassicControllerExtensionEvent CCEvt;

            CCEvt = (WRClassicControllerExtensionEvent)evt;

            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.A) ) {
                System.out.println("A!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.B) ) {
                System.out.println("B!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.Y) ) {
                System.out.println("Y!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.X) ) {
                System.out.println("X!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.LEFT_Z) ) {
                System.out.println("ZL!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.RIGHT_Z) ) {
                System.out.println("ZR!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.LEFT_TRIGGER) ) {
                System.out.println("TL!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.RIGHT_TRIGGER) ) {
                System.out.println("TR!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_LEFT) ) {
                System.out.println("DL!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_RIGHT) ) {
                System.out.println("DR!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_UP) ) {
                System.out.println("DU!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_DOWN) ) {
                System.out.println("DD!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.PLUS) ) {
                System.out.println("Plus!");
            }
            if ( CCEvt.wasPressed(WRClassicControllerExtensionEvent.MINUS) ) {
                System.out.println("Minus!");
            }
            if ( CCEvt.isPressed(WRClassicControllerExtensionEvent.HOME) ) {
                System.out.println("L shoulder: " + CCEvt.getLeftTrigger());
                System.out.println("R shoulder: " + CCEvt.getRightTrigger());
            }
        }
    }
    
    public void extensionConnected(WiiRemoteExtension extension)
    {
        System.out.println("Extension connected!");
        try {
            remote.setExtensionEnabled(true);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    public void extensionPartiallyInserted()
    {
        System.out.println("Extension partially inserted. Please push it in more!");
    }
    
    public void extensionUnknown()
    {
        System.out.println("Extension unknown. Did you try to plug in a toaster or something?");
    }

    public void extensionDisconnected(WiiRemoteExtension extension)
    {
        System.out.println("Extension disconnected. Why'd you unplug it?");
    }

    private void mouseCycle()
    {
        try {
            if ( status == 0 ) {
                if ( accelerometerStatus == 0 ) {
                    remote.setMouse(MotionAccelerometerMouse.getDefault());
                }
                else if ( accelerometerStatus == 1 ) {
                    remote.setMouse(TiltAccelerometerMouse.getDefault());
                }
                else if ( accelerometerStatus == 2 ) {
                    remote.setMouse(new MotionAccelerometerMouse(80, 60, AccelerometerMouse.NUNCHUK_EXTENSION, 0.06, 0.08));
                }
                else if ( accelerometerStatus == 3 ) {
                    remote.setMouse(new TiltAccelerometerMouse(10, 10, AccelerometerMouse.NUNCHUK_EXTENSION, 0.1, 0.1));
                }
            }
            else if ( status == 1 ) {
                remote.setMouse(IRMouse.getDefault());
            }
            else if ( status == 2 ) {
                remote.setMouse(IRAccelerometerMouse.getDefault());
            }
            else if ( status == 3 ) {
                if ( analogStickStatus == 0 ) {
                    remote.setMouse(AbsoluteAnalogStickMouse.getDefault());
                }
                else if ( analogStickStatus == 1 ) {
                    remote.setMouse(RelativeAnalogStickMouse.getDefault());
                }
                else if ( analogStickStatus == 2 ) {
                    remote.setMouse(new AbsoluteAnalogStickMouse(1, 1, AnalogStickMouse.CLASSIC_CONTROLLER_LEFT));
                }
                else if ( analogStickStatus == 3 ) {
                    remote.setMouse(new RelativeAnalogStickMouse(10, 10, 0.05, 0.05, AnalogStickMouse.CLASSIC_CONTROLLER_LEFT));
                }
                else if ( analogStickStatus == 4 ) {
                    remote.setMouse(new AbsoluteAnalogStickMouse(1, 1, AnalogStickMouse.CLASSIC_CONTROLLER_RIGHT));
                }
                else if ( analogStickStatus == 5 ) {
                    remote.setMouse(new RelativeAnalogStickMouse(10, 10, 0.05, 0.05, AnalogStickMouse.CLASSIC_CONTROLLER_RIGHT));
                }
            }
            mouseTestPanel.repaint();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }            
    }
    
    public void buttonInputReceived(WRButtonEvent evt)
    {
        if ( evt.wasPressed(WRButtonEvent.TWO) ) {
            System.out.println("2");
        }
        if ( evt.wasPressed(WRButtonEvent.ONE) ) {
            System.out.println("1");
        }
        if ( evt.wasPressed(WRButtonEvent.B) ) {
            System.out.println("B");
        }
        if ( evt.wasPressed(WRButtonEvent.A) ) {
            System.out.println("A");
        }
        if ( evt.wasPressed(WRButtonEvent.MINUS) ) {
            System.out.println("Minus");
        }
        if ( evt.wasPressed(WRButtonEvent.HOME) ) {
            System.out.println("Home");
        }
        if ( evt.wasPressed(WRButtonEvent.LEFT) ) {
            System.out.println("Left");
        }
        if ( evt.wasPressed(WRButtonEvent.RIGHT) ) {
            System.out.println("Right");
        }
        if ( evt.wasPressed(WRButtonEvent.DOWN) ) {
            System.out.println("Down");
        }
        if ( evt.wasPressed(WRButtonEvent.UP) ) {
            System.out.println("Up");
        }
        if ( evt.wasPressed(WRButtonEvent.PLUS) ) {
            System.out.println("Plus");
        }
        
        try {
            //if (evt.isPressed(WRButtonEvent.MINUS) && evt.wasPressed(WRButtonEvent.PLUS))
            //System.out.println("Avg Tardiness: " + remote.totalTardiness/remote.reportsProcessed);
            
            if ( evt.isPressed(WRButtonEvent.HOME) ) {
                boolean lightChanged = false;
                if ( evt.wasPressed(WRButtonEvent.PLUS) && !mouseTestingOn ) {
                    mouseTestingOn = true;
                    remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.B, java.awt.event.InputEvent.BUTTON1_MASK));
                    remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.A, java.awt.event.InputEvent.BUTTON3_MASK));
                    remote.getButtonMaps().add(new ButtonMouseWheelMap(WRButtonEvent.UP, -5, 100));
                    remote.getButtonMaps().add(new ButtonMouseWheelMap(WRButtonEvent.DOWN, 5, 100));
                    mouseTestFrame.setVisible(true);
                    mouseCycle();
                }
                else if ( evt.wasPressed(WRButtonEvent.MINUS) && mouseTestingOn ) {
                    mouseTestingOn = false;
                    remote.getButtonMaps().remove(new ButtonMouseMap(WRButtonEvent.B, java.awt.event.InputEvent.BUTTON1_MASK));
                    remote.getButtonMaps().remove(new ButtonMouseMap(WRButtonEvent.A, java.awt.event.InputEvent.BUTTON3_MASK));
                    remote.getButtonMaps().remove(new ButtonMouseWheelMap(WRButtonEvent.UP, -5, 100));
                    remote.getButtonMaps().remove(new ButtonMouseWheelMap(WRButtonEvent.DOWN, 5, 100));
                    mouseTestFrame.setVisible(false);
                    remote.setMouse(null);
                }
                else if ( evt.wasPressed(WRButtonEvent.ONE) ) {
                    accelerometerSource = !accelerometerSource;
                    if (accelerometerSource)graphFrame.setTitle("Accelerometer graph: Wii Remote");
                    else graphFrame.setTitle("Accelerometer graph: Nunchuk");
                }
                else if ( evt.wasPressed(WRButtonEvent.TWO) ) {
                    // Code for Wii Remote memory dump/comparison
                    Thread thread = new Thread(new MyFileThread(this));
                    thread.start();
                }
                else if ( mouseTestingOn ) {
                    boolean change = false;
                    if ( evt.wasPressed(WRButtonEvent.HOME+WRButtonEvent.RIGHT) ) {
                        status = (status+1)%4;
                        change = true;
                    }
                    else if ( evt.wasPressed(WRButtonEvent.HOME+WRButtonEvent.LEFT) ) {
                        status = (status+3)%4;
                        change = true;
                    }
                    
                    if ( status == 0 ) {
                        if ( evt.wasPressed(WRButtonEvent.DOWN) ) {
                            accelerometerStatus = (accelerometerStatus+1)%4;
                            change = true;
                        }
                        else if ( evt.wasPressed(WRButtonEvent.UP) ) {
                            accelerometerStatus = (accelerometerStatus+3)%4;
                            change = true;
                        }
                    }
                    else if ( status == 3 ) {
                        if ( evt.wasPressed(WRButtonEvent.DOWN) ) {
                            analogStickStatus = (analogStickStatus+1)%6;
                            change = true;
                        }
                        else if (evt.wasPressed(WRButtonEvent.UP)) {
                            analogStickStatus = (analogStickStatus+5)%6;
                            change = true;
                        }
                    }
                    
                    if ( change ) {
                        mouseCycle();
                    }
                }
            }
            else if ( evt.wasPressed(WRButtonEvent.TWO) ) {
                remote.requestStatus();
                if ( remote.isPlayingSound() ) {
                    remote.stopSound();
                }
            }
            else if ( evt.wasPressed(WRButtonEvent.ONE) ) {
                if ( prebuf != null ) {
                    remote.playPrebufferedSound(prebuf, WiiRemote.SF_PCM8S);
                }
            }
            else if ( evt.wasPressed(WRButtonEvent.PLUS) ) {
                if ( remote.isSpeakerEnabled() ) {
                    double volume = (remote.getSpeakerVolume()*20+1)/20;
                    if ( volume <= 1 ) {
                        remote.setSpeakerVolume(volume);
                    }
                    System.out.println("Volume: " + remote.getSpeakerVolume());
                }
            }
            else if ( evt.wasPressed(WRButtonEvent.MINUS) ) {
                if ( remote.isSpeakerEnabled() ) {
                    double volume = (remote.getSpeakerVolume()*20-1)/20;
                    if ( volume >= 0 ) {
                        remote.setSpeakerVolume(volume);
                    }
                    System.out.println("Volume: " + remote.getSpeakerVolume());
                }
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
