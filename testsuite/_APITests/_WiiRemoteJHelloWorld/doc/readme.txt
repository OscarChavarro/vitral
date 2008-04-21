===========================================================================

This program contains a simple code template for accesing a Nintendo Wii
remote from a PC with a USB/Bluetooth dongle and linux.

What you need dor this to work:
  - A Nintendo Wii remote
  - A USB/Bluetooth dongle antena
  - Linux operating system with bluetooth enabled kernel and "bluez" library
    (as of october 2007, internet users report that you have to pay for
    bluetooth programming tools on windows... Author (Oscar Chavarro) didn't
    search for Windows support)
  - Some bluetooth stack library such as Avetana or Bluecove for accessing
    bluez from java (tested in linux with both, Avetana and Bluecove)
  - A JDK (tested with 1.6)
  - Vitral SDK :)

Procedure for making this work:
  - Fedora core 7 has bluetooth activated by default, so no step is required
    here
  - Install Bluez:
    yum install bluez-utils
  - Check to see if Linux sees the dongle. 
    If everything goes well, the "hciconfig" command reports the dongle as
    a networking device similar to "ifconfig": 
   
/usr/sbin/hciconfig
hci0:   Type: USB
        BD Address: 00:16:38:C9:4E:F6 ACL MTU: 1017:8 SCO MTU: 64:0
        UP RUNNING PSCAN 
        RX bytes:3679007 acl:118657 sco:0 events:235 errors:0
        TX bytes:4124 acl:140 sco:0 commands:65 errors:0

    an empty report means "no dongle founded"
  - Turn on Nintendo Wii remote, and press "1" and "2" buttons simultanously
    to turn control on and set it in connect mode.  If any Wii box is near,
    it must be turned off beforehand.
  - Execute the command "hcitool scan" while 4 blue leds on Wii remote are
    blinking. If everything goes well, output goes as follows:

hcitool scan
Scanning ...
        00:19:FD:C6:51:06       Nintendo RVL-CNT-01

    if only "Scanning ..." appears, this means Nintendo Wii remote is not
    connected.
  - The ./compile.sh script assumes you have "avetanaBT.jar" (or bluecove) and
    "WiiRemoteJ.jar" installed on  Java's extension directory (jre/lib/ext 
    - jdk/jre/lib/ext) and that previous tests are working fine.
  - Run this program (after compiling it with ./compile.sh)
    ./run.sh
    and press Nintendo Wii remotes buttons "1" and "2" again.
    Here program should correctly detect Nintendo Wii remote and plot
    graphs for 3 accelerometer axis.

:)

===========================================================================
= EOF                                                                     =
===========================================================================
