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
import org.ccnx.ccn.io.CCNReader;
import org.ccnx.ccn.io.CCNWriter;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;


import org.ccnx.ccn.utils.CommonSecurity;


public class CCNDataReaderReg {

    private static final int BUF_SIZE = 2048;
    private static final String _strPrefix = "ccnx:/test/push/";
    private static final long ONEDAY = 1000L * 60 * 60 * 24;

    private ContentName _requestName = null;
    private ContentName _prefix = null;
    

    public String read(String request){
        try{
            ContentName contentName = ContentName.fromURI(request);
            Interest interest = new Interest(contentName);
            System.out.println("**************" + contentName.toURIString() + "\n" + "***************" + _strPrefix + ", " + request);
            CCNHandle handle = CCNHandle.open();
            CCNReader reader = new CCNReader(handle);
            System.out.println("************ before hermesGet");
            ContentObject co = reader.get(interest, ONEDAY);
            String ans = new String(co.content());
            System.out.println("***************Got data : " + ans);
            return ans;
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
        CCNDataReaderReg reader = new CCNDataReaderReg();
        
        long curTime = System.currentTimeMillis();
        reader.read(_strPrefix + "Alice");
    }
}
