/**
 * A multi-threaded static web server
 *
 * Default settings:
 *   main content directory is "content"
 *   defaults to checking for an "index.html" file
 *
 * user settings can be loaded in from a "settings.cfg"
 *
 * original code created by Hardik Patel- 2/25/14
 *
 * Updated code by Blake McCulley
 */

// needs to be updated to only include 
// necessary imports
import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer
{
    /**
     * The main method of the webserver, gets everything up 
     * and going.
     * <p>
     * Currently accepts no arguments, as I felt that everything
     * that needs to be set/configured could be done in the 
     * setting.cfg file. Maybe this needs to be changed? Should
     * certain settings be available directly from the command line?
     * <p>
     * Reads in the settings from setting.cfg, sets up a listen port.
     * And starts the HttpRequest class.
     *
     * @param argv 
     *           Currently nothing accepted for this
     * @throws java.lang.Exception
     *           Throws an exception if an error is encountered
     */
    public static void main(String argv[]) throws Exception
    {
        //Set default port number
        int port = 80;
        
        // settings to be loaded
        String contentDirectory = "content";
		    String defaultFile = "index.html";
		    String error404File = null;
		    Boolean listDirectory = false;
        
        // load in the settings
        Properties cfgSettings = new Properties();
        InputStream cfgIn = null;
        
        try
        {
            cfgIn = new FileInputStream("settings.cfg");
            cfgSettings.load(cfgIn);
            // set to run on a different port
            String portstr = cfgSettings.getProperty("port");
            if(portstr != null && !portstr.isEmpty()) {
            	port = Integer.parseInt(portstr);
            }
            contentDirectory = cfgSettings.getProperty("contentDirectory");
            defaultFile = cfgSettings.getProperty("defaultFile");
            listDirectory = Boolean.valueOf(cfgSettings.getProperty("listFiles"));
            //error404File = cfgSettings.getProperty("error404File");
        }
        catch (IOException ex) {
            // log error
            ex.printStackTrace();
        }
        
        //Establish listen socket
        ServerSocket listenSocket = new ServerSocket(port);
        
        System.out.println("Starting server on port "+port+"\n");
        
        //Process HTTP service requests in an infinite loop
        while(true)
        {
            //Listen for a TCP connection request
            Socket TCPsocket = listenSocket.accept();
            
            //Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(TCPsocket, contentDirectory, defaultFile, listDirectory);
            
            //Create a new thread to process the request.
            Thread thread = new Thread(request);
            
            System.out.println("Starting server...\n");
            
            //Start the thread.
            thread.start();
            
        }
    }
}
