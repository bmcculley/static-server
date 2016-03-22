/**
 * A multi-threaded static web server
 *
 * original code created by Hardik Patel- 2/25/14
 *
 * Updated code by Blake McCulley
 */


import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer
{
    public static void main(String argv[]) throws Exception
    {
        //Set default port number
        int port = 80;
        
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
            HttpRequest request = new HttpRequest(TCPsocket);
            
            //Create a new thread to process the request.
            Thread thread = new Thread(request);
            
            System.out.println("Starting server...\n");
            
            //Start the thread.
            thread.start();
            
        }
    }
}
