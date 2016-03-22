/**
 * handles the main processing for the web server
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
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

public final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;
    String contentDirectory = "content";
    String defaultFile = "index.html";
    String error404File = null;
    Boolean listDirectory = false;
    
    //Constructor
    public HttpRequest(Socket socket) throws Exception
    {
        this.socket = socket;
        // load in the settings
        Properties cfgSettings = new Properties();
        InputStream cfgIn = null;
        
        try
        {
            cfgIn = new FileInputStream("settings.cfg");
            cfgSettings.load(cfgIn);
            // check if setting exists, if it does load it
            contentDirectory = cfgSettings.getProperty("contentDirectory");
            defaultFile = cfgSettings.getProperty("defaultFile");
            listDirectory = Boolean.valueOf(cfgSettings.getProperty("listFiles"));
            //error404File = cfgSettings.getProperty("error404File");
        }
        catch (IOException ex) {
            // log error
            ex.printStackTrace();
        }
        finally
        {
            if (cfgIn != null)
            {
                try
                {
                    cfgIn.close();
                }
                catch (IOException e)
                {
                    // log error
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    //Impplement the run() method of the Runnable interface
    public void run()
    {
        try
        {
            processRequest();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
    
    public void walk( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );
                System.out.println( "Dir:" + f.getAbsolutePath() );
                
            }
            else {
            		System.out.println( "File:" + f.getAbsolutePath() );
            		
            }
        }
    }
    
    private void processRequest() throws Exception
    {
        // Get a reference to the socket's input and output streams.
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        // Set up input stream filters.
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        
        // Get the request line of the HTTP request message.
        String requestLine = br.readLine();
        
        // Display the request line.
        System.out.println();
        System.out.println(requestLine);
        
        // Get and display the header lines.
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0)
        {
            System.out.println(headerLine);
        }
        
        //Extract the filename from the request line
        StringTokenizer tokens = new StringTokenizer(requestLine);
        
        //Skip over the method, which should be "GET"
        tokens.nextToken();
        
        String fileName = tokens.nextToken();
        String origFileName = "";

        // if no path (filename) set to index
        if (fileName.trim().length() == 0 || fileName.equals("/"))
        {
            fileName = "/" + defaultFile;
        }

        // add the content directory to the filename
        fileName = "./" + contentDirectory + fileName;
        
        //Open the requested file
        FileInputStream fis = null;
        boolean fileExists = true;
        
        File testFile = new File(fileName);
        
        if ( testFile.isFile() )
        {
        		// idk?
        }
        else if ( testFile.isDirectory() )
        {
        		origFileName = fileName;
        		fileName = fileName + "/" + defaultFile;
        }
        
        try
        {		
        		fis = new FileInputStream(fileName);
        }
        catch(FileNotFoundException e)
        {
            fileExists = false;
        }
        
        //Construct the response message
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        
        if(fileExists)
        {
            statusLine = "HTTP/1.1 200 OK: ";
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        }
        else if (testFile.isDirectory() && listDirectory)
        {
        		// need to check if directory contents should be listed and...
        		// also check if there is an index file to display
        		statusLine = "HTTP/1.1 200 OK: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<!DOCTYPE html>" + "<head><title>Directory Listing</title></head>" + "<body><p>List Directory Contents</p></body></html>";
            
            walk(origFileName);
        }
        else
        {
            statusLine = "HTTP/1.1 404 Not Found: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<!DOCTYPE html>" + "<head><title>Page Not Found</title></head>" + "<body><h1>Something went all wonky there</h1><p>Page not Found</p></body></html>";
        }
        
        //Send the status line
        os.writeBytes(statusLine);
        
        //Send the content type line
        os.writeBytes(contentTypeLine);
        
        //Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);
        
        //Send the entity body
        if(fileExists)
        {
            sendBytes(fis, os);
            fis.close();
        }
        else
        {
            os.writeBytes(entityBody);
        }
        
        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
    }
    
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        //Construct a 1K buffer to hold bytes on their way to the socket
        byte[] buffer = new byte[1024];
        int bytes = 0;
        
        //Copy requested file into the socket's output stream
        while((bytes = fis.read(buffer)) != -1)
        {
            os.write(buffer, 0, bytes);
        }
    }
    
    private static String contentType(String fileName)
    {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html") || fileName.endsWith(".xhtml"))
        {
            return "text/html";
        }
        else if (fileName.endsWith(".css"))
        {
            return "text/css";
        }
        else if (fileName.endsWith(".js"))
        {
            return "text/js";
        }
        else if (fileName.endsWith(".gif"))
        {
            return "image/gif";
        }
        else if (fileName.endsWith(".jpg"))
        {
            return "image/jpeg";
        }
        else if (fileName.endsWith(".png"))
        {
            return "image/png";
        }
        else
        {
        		return "application/octet-stream";
        }
    }
}