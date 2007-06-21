This directory contains three (3) applications:
  - BatchConsole: it is the simplest of all interfaces to the search engine.
    It is supposed to be used for simple tests with small number of models
    (less than 10k).  It must allways read the full database before any
    operation, so it is slow. Use it for testing purposes.
  - ServerDaemon: a modified and improved version of BatchConsole which only
    load the database once (at startup), and after that enters in a cicle of
    recieving network connections over which commands can be sent. This program
    generates an end user formatted output in an html file.
  - NetworkClientConsole: simple text console for ServerDaemon
