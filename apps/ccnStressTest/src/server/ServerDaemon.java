package hermes.server;

import java.util.*;
import java.io.*;

import org.ccnx.ccn.protocol.*;
import org.ccnx.ccn.config.*;


public class ServerDaemon{
    

    //TODO: load the password information from an encryped file

    public static void main(String argv[]){
        try{
            CCNDataWriter writer = new CCNDataWriter();
            writer.start();
        }
        catch(MalformedContentNameStringException ex){
            System.out.println("MalformedContentNameStringException in ServerDaemon-Main: " + ex.getMessage());
            ex.printStackTrace();
        }    
        catch(IOException ex){
            System.out.println("IOException in ServerDaemon-Main: " + ex.getMessage());
            ex.printStackTrace();
        }
        catch(ConfigurationException ex){
            System.out.println("ConfigurationException in ServerDaemon-Main: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
