package hermes.client;

import hermes.util.*;

import java.util.*;
import java.sql.Timestamp;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.ccnx.ccn.CCNFilterListener;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNFileInputStream;
import org.ccnx.ccn.io.CCNInputStream;
import org.ccnx.ccn.io.CCNOutputStream;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;


import org.ccnx.ccn.utils.CommonSecurity;


public class CCNQuerySender {

    private static final int BUF_SIZE = 2048;
    private static final String _strPrefix = "ccnx:/test/push/";

    private ContentName _requestName = null;
    private ContentName _prefix = null;


    public String sendQuery(String strQuest){
        try{
            ContentName requestName = ContentName.fromURI(strQuest);
            System.out.println("**************" + requestName.toURIString() + "\n" + "***************" + _strPrefix + ", " + strQuest);
            CCNHandle handle = CCNHandle.open();
            CCNInputStream input = new CCNInputStream(requestName, handle);

            byte [] buffer = new byte[BUF_SIZE];
            ByteKeeper byteResponse = new ByteKeeper();

            int readCnt = 0;
            int readTotal = 0;
            readCnt = input.read(buffer);
            while(readCnt >= 0){
                System.out.println("In reading : readCnt is " + readCnt + "\n");
                byteResponse.append(buffer, readCnt);
                readTotal = readTotal + readCnt;
                readCnt = input.read(buffer);
            }
            input.close();
            handle.close();
            System.out.println("Read Done!\n");
            String strResponse = new String(byteResponse.getBytes());
            System.out.println("Received string is : " + strResponse);
            //do not need a make valid function since there's no restrictions on the format of response
            return strResponse;
        }
        catch (ConfigurationException e) {
            System.out.println("ConfigurationException in CCNQuerySender-sendQuery: " + e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("IOException in CCNQuerySender-sendQuery: " + e.getMessage());
            e.printStackTrace();
        }
        catch (MalformedContentNameStringException e) {
            System.out.println("MalformedContentNameStringException in CCNQuerySender-sendQuery : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
    
    public static void main(String argv[]){
        CCNQuerySender sender = new CCNQuerySender();
        
        long curTime = System.currentTimeMillis();
        sender.sendQuery(_strPrefix + "Alice");
    }
}
