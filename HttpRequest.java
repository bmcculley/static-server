/**
 * handles the main processing for the web server
 *  
 * original code created by Hardik Patel- 2/25/14
 *
 * Updated code by Blake McCulley
 *
 */
 
// needs to be updated to only include 
// necessary imports
import java.io.*;
import java.net.*;
import java.util.*;

public final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;
    String contentDirectory;
    String defaultFile;
    String error404File;
    Boolean listDirectory;
    
    /**
     * Constructor - creates the HttpRequest object
     *
     * @param socket 
     *           expects to recieve an TCPsocket port to run on
     * @param contentDirectory 
     *           Default directory were HTML content will be served from.
     *           (type String)
     * @param defaultFile 
     *           Default file that will be looked for and served.
     *           (type String)
     * @param listDirectory 
     *           Boolean value of whether to list directory contents.
     * @throws java.lang.Exception
     *           Throws an exception if an error is encountered
     */
    public HttpRequest( Socket socket,
                        String contentDirectory,
                        String defaultFile,
                        Boolean listDirectory)
                    throws Exception
    {
        this.socket = socket;
        this.contentDirectory = contentDirectory;
        this.defaultFile = defaultFile;
        this.listDirectory = listDirectory;
    }
    
    /**
     * Impplement the run() method of the Runnable interface
     * <p>
     * Fires up the <code>processRequest</code> method.
     *
     */
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
    
    /**
     * Walk the directory provided in the URL path
     * <p>
     * If listDirectory is set to true 
     * in the <code>settings.cfg</code> this should return the 
     * directory contents as an unordered list to be shown to the end user.
     *
     * @param path URL path to currently requested directory as String
     * @return HTML unordered list of files and directories
     */
    public String walk( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();
        String listing = "<ul>";

        if (list == null)
        {
        		// maybe this should throw an error instead
        		return "";
				}
				else
				{
		        for ( File f : list )
		        {
		            if ( f.isDirectory() )
		            {
		                walk( f.getAbsolutePath() );
		                System.out.println( "Dir:" + f.getAbsolutePath() );
		                listing += "<li>Dir:" + f.getAbsolutePath()+"</li>";
		            }
		            else
		            {
		            		System.out.println( "File:" + f.getAbsolutePath() );
		            		listing += "<li>File:" + f.getAbsolutePath()+"</li>";
		            }
		        }
		        listing += "</ul>";
		        return listing;
		    }
    }
    
    /**
     * Does all of the heavy lifting.
     *
     * Get the HTTP request, outputs this to the command line 
     * Determines if the file/path exists, and whether it should
     * append the default file name.
     * <p>
     * Finally it determines the proper output response and delivers
     * that back to the socket.
     * 
     * TODO: Add properly formatted logging. Location of log file 
     * should be determined by admin and set in <code>settings.cfg</code>
     *
     */
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
        System.out.println("\n" + requestLine);
        
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
            entityBody = "<!DOCTYPE html>" + "<head><title>Directory Listing</title></head>" + 
            						 "<body><h1>List Directory Contents</h1><hr/>"+walk(origFileName)+
            						 "<hr/><p>Served by <a href=\"#!\">xyz</a></p></body></html>";
            
        }
        else
        {
            statusLine = "HTTP/1.1 404 Not Found: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<!DOCTYPE html>" + "<head><title>Page Not Found</title></head>" + 
                         "<body><h1>Something went all wonky there</h1><p>Page not Found</p></body></html>";
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
    
    /**
     * Send the bytes!
     *
     */
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
    
    /**
     * Returns the content MIME header based on file name
     *
     * Parses the file location string and tries to determine the file type 
     * based on the file externsion.
     *
     * @param fileName file location string with file extension.
     * @return Header MIME type
     */
    public static String contentType(String fileName)
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