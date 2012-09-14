package hermes.server;

import hermes.util.*;

import java.util.*;
import java.lang.*;
import org.ccnx.ccn.io.CCNVersionedOutputStream;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.ccnx.ccn.CCNFilterListener;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.*;
import org.ccnx.ccn.profiles.CommandMarker;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.VersioningProfile;
import org.ccnx.ccn.profiles.metadata.MetadataProfile;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse.NameEnumerationResponseMessage;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse.NameEnumerationResponseMessage.NameEnumerationResponseMessageObject;
import org.ccnx.ccn.profiles.security.KeyProfile;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;

public class CCNQueryListener implements CCNFilterListener{
    private static final int BUF_SIZE = 2048;

    private String _baseDir = null;
    private String _strPrefix = "ccnx:/test/push/";
    private Interest _interest = null;
    private CCNHandle _handle = null;
    private ContentName _prefix = null;


    
    public CCNQueryListener() throws MalformedContentNameStringException, ConfigurationException, IOException {
        _prefix = ContentName.fromURI(_strPrefix);
        _handle = CCNHandle.open();
    }


    private boolean transmitData(ByteKeeper response, Interest interest, CCNHandle handle){
        //CCNTime birthTime = new CCNTime(data.getBirthTime());

        try{
            CCNOutputStream ccnout = new CCNOutputStream(interest.name(), handle);

            //register the outstanding interest
            ccnout.addOutstandingInterest(interest);

            byte [] buffer = new byte[BUF_SIZE];
            int offset = 0;
            int readCnt = response.read(buffer, BUF_SIZE, offset);
            while (readCnt >0){
                ccnout.write(buffer, 0, readCnt);
                offset = offset + readCnt;
                readCnt = response.read(buffer, BUF_SIZE, offset);
            }
            ccnout.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
            return false;
        }
        return true;

    }

    public void start() throws IOException{
        // All we have to do is say that we're listening on our main prefix.
        _handle.registerFilter(_prefix, this);
    }



    public boolean handleInterest(Interest interest) {
        System.out.println("received Interest : " + interest.name().toURIString() + "\n");

        
        if (SegmentationProfile.isSegment(interest.name()) && !SegmentationProfile.isFirstSegment(interest.name())) {
            System.out.println("Got an interest for something other than a first segment, ignoring : " + interest.name().toURIString());
            return false;
        } 
        else if (MetadataProfile.isHeader(interest.name())) {
            System.out.println("Got an interest for the first segment of the header, ignoring : " + interest.name().toURIString());
            return false;
        }
        String strResponse = "Shen Test!!!";
        ByteKeeper byteResponse = new ByteKeeper(strResponse.getBytes());
        return transmitData(byteResponse, interest, _handle);
    }



    /**
     * Turn off everything.
     * @throws IOException 
     */
    public void shutdown() throws IOException {
        if (null != _handle) {
            _handle.unregisterFilter(_prefix, this);
            System.out.println("CCNQueryListener Closed!\n");
        }
    }




}


