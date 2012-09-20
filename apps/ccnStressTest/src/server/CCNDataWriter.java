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
import  java.security.SignatureException;

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

public class CCNDataWriter implements CCNFilterListener{
    private static final Integer CONTENT_LIFE_TIME = 1000 * 60;

    private String _baseDir = null;
    private String _strPrefix = "ccnx:/test/push/";
    private Interest _interest = null;
    private CCNHandle _handle = null;
    private ContentName _prefix = null;

    
    public CCNDataWriter() throws MalformedContentNameStringException, ConfigurationException, IOException {
        _prefix = ContentName.fromURI(_strPrefix);
        _handle = CCNHandle.open();
    }


    private boolean write(String response, Interest interest){
        //CCNTime birthTime = new CCNTime(data.getBirthTime());

        try{
            CCNWriter writer = new CCNWriter(_handle);

            //register the outstanding interest
            //ccnout.addOutstandingInterest(interest);
            writer.addOutstandingInterest(interest);
            //writer.put(interest.getContentName(), response.getBytes());
            writer.hermesPut(interest.getContentName(), response.getBytes(), CONTENT_LIFE_TIME, interest);
            writer.close();
        }
        catch(SignatureException ex){
            ex.printStackTrace();
            return false;
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
        System.out.println("===========================received Interest : " + interest.name().toURIString() + "\n");

        
        if (SegmentationProfile.isSegment(interest.name()) && !SegmentationProfile.isFirstSegment(interest.name())) {
            System.out.println("Got an interest for something other than a first segment, ignoring : " + interest.name().toURIString());
            return false;
        } 
        else if (MetadataProfile.isHeader(interest.name())) {
            System.out.println("Got an interest for the first segment of the header, ignoring : " + interest.name().toURIString());
            return false;
        }
        String strResponse = "Shen Test!!!";
        return write(strResponse, interest);
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


