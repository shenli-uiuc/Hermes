package com.parc.ccn.library;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import com.parc.ccn.CCNBase;
import com.parc.ccn.data.CompleteName;
import com.parc.ccn.data.ContentName;
import com.parc.ccn.data.ContentObject;
import com.parc.ccn.data.MalformedContentNameStringException;
import com.parc.ccn.data.content.Link;
import com.parc.ccn.data.query.CCNFilterListener;
import com.parc.ccn.data.query.CCNInterestListener;
import com.parc.ccn.data.query.ExcludeFilter;
import com.parc.ccn.data.query.Interest;
import com.parc.ccn.data.security.ContentAuthenticator;
import com.parc.ccn.data.security.KeyLocator;
import com.parc.ccn.data.security.LinkAuthenticator;
import com.parc.ccn.data.security.PublisherKeyID;
import com.parc.ccn.data.security.ContentAuthenticator.ContentType;
import com.parc.ccn.security.keys.KeyManager;

/**
 * Higher-level interface to CCNs.
 * @author smetters
 * 
 * <META> tag under which to store metadata (either on name or on version)
 * <V> tag under which to put versions
 * n/<V>/<number> -> points to header
 * <B> tag under which to put actual fragments
 * n/<V>/<number>/<B>/<number> -> fragments
 * n/<latest>/1/2/... has pointer to latest version
 *  -- use latest to get header of latest version, otherwise get via <v>/<n>
 * configuration parameters:
 * blocksize -- size of chunks to fragment into
 * 
 * get always reconstructs fragments and traverses links
 * can getLink to get link info
 *
 */
public interface CCNLibrary extends CCNBase {
	
	public enum OpenMode { O_RDONLY, O_WRONLY };

	public void setKeyManager(KeyManager keyManager);

	public KeyManager keyManager();
		
	public PublisherKeyID getDefaultPublisher();

	public CompleteName put(ContentName name, byte [] contents) throws SignatureException, IOException, InterruptedException;

	/**
	 * Publish a piece of content under a particular identity.
	 * All of these automatically make the final name unique.
	 * @param name
	 * @param contents
	 * @param publisher selects one of our identities to publish under
	 * @throws SignatureException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public CompleteName put(ContentName name, byte [] contents,
							PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;
	
	public CompleteName put(String name, String contents) throws SignatureException, MalformedContentNameStringException, IOException, InterruptedException;
	
	public CompleteName put(
			ContentName name, 
			byte[] contents, 
			ContentAuthenticator.ContentType type,
			PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;

	public CompleteName put(
			ContentName name, 
			byte [] contents,
			ContentAuthenticator.ContentType type,
			PublisherKeyID publisher, KeyLocator locator,
			PrivateKey signingKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException, InterruptedException;
	
	// internal functions about fragmentation - may be exposed, or in std impl
	
	public CompleteName newVersion(ContentName name,
								   byte [] contents) throws SignatureException, IOException, InterruptedException;
	public CompleteName newVersion(ContentName name,
								   byte [] contents, 
								   PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;
	public CompleteName newVersion(
			ContentName name, 
			byte[] contents,
			ContentType type, // handle links and collections
			PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;
	
	/**
	 * Generates the complete name for this piece of leaf content. 
	 * @param name The base name to version.
	 * @param version The version to publish.
	 * @param contents The (undigested) contents. Must be smaller than the fragmentation threshold for now.
	 * @param type The desired type, or null for default.
	 * @param publisher The desired publisher, or null for default.
	 * @param locator The desired key locator, or null for default.
	 * @param signingKey The desired signing key, or null for default.
	 * @return
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public CompleteName newVersionName(
			ContentName name, int version, byte [] contents,
			ContentType type,
			PublisherKeyID publisher, KeyLocator locator,
			PrivateKey signingKey) throws SignatureException, 
			InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException;

	public CompleteName addVersion(
			ContentName name, 
			int version, 
			byte [] contents,
			ContentType type,
			PublisherKeyID publisher, KeyLocator locator,
			PrivateKey signingKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException;
	
	public ContentObject get(ContentName name, long timeout) throws IOException, InterruptedException;
	
	/*
	 * Experimental interface - may be deprecated in the future
	 */
	public ContentObject get(Interest interest, long timeout) throws IOException, InterruptedException;

	/**
	 * Get the latest version published by this publisher,
	 * or by anybody if publisher is null.
	 */
	public ContentName getLatestVersionName(ContentName name, PublisherKeyID publisher);

	/**
	 * Return the numeric version associated with this
	 * name.
	 * @param name
	 * @return version or -1 if no recognizable version information.
	 */
	public int getVersionNumber(ContentName name);
	
	/**
	 * Compute the name of this version.
	 * @param name
	 * @param version
	 * @return
	 */
	public ContentName versionName(ContentName name, int version);

	/**
	 * Does this name represent a version of the given parent?
	 * @param version
	 * @param parent
	 * @return
	 */
	public boolean isVersionOf(ContentName version, ContentName parent);
	
	public boolean isVersioned(ContentName name);
	
	/**
	 * Things are not as simple as this. Most things
	 * are fragmented. Maybe make this a simple interface
	 * that puts them back together and returns a byte []?
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public ContentObject getLatestVersion(ContentName name, 
										  PublisherKeyID publisher) throws IOException, InterruptedException;

	/**
	 * Does this specific name point to a link?
	 * Looks at local (cached) data only. 
	 * If more than one piece of content matches
	 * this CompleteName, returns false.
	 * @param name
	 * @return true if its a link, false if not. 
	 */
	public boolean isLink(CompleteName name);
	
	/**
	 * Return the link itself, not the content
	 * pointed to by a link. 
	 * @param name the identifier for the link to work on
	 * @return returns null if not a link, or name refers to more than one object
	 * @throws SignatureException
	 * @throws IOException
	 */
	public ContentObject getLink(CompleteName name);
	
	public CompleteName link(ContentName src, ContentName dest, 
							 LinkAuthenticator destAuthenticator) throws SignatureException, IOException, InterruptedException;
	public CompleteName link(ContentName src, ContentName dest, 
							 LinkAuthenticator destAuthenticator, PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;
	public CompleteName link(ContentName src, ContentName dest,
			LinkAuthenticator destAuthenticator, 
			PublisherKeyID publisher, KeyLocator locator,
			PrivateKey signingKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException, InterruptedException;
	
	public CompleteName addCollection(ContentName name, Link [] contents) throws SignatureException, IOException, InterruptedException;
	public CompleteName addCollection(ContentName name, Link [] contents, 
									  PublisherKeyID publisher) throws SignatureException, IOException, InterruptedException;
	public CompleteName addCollection(ContentName name, 
			Link[] contents,
			PublisherKeyID publisher, KeyLocator locator,
			PrivateKey signingKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException, InterruptedException;
	
	/**
	 * Use the same publisherID that we used originally.
	 */
	public CompleteName addToCollection(ContentName name, CompleteName [] additionalContents);
	public CompleteName removeFromCollection(ContentName name, CompleteName [] additionalContents);

	public void expressInterest(
			Interest interest,
			CCNInterestListener listener) throws IOException;
	
	public void cancelInterest(Interest interest, CCNInterestListener listener) throws IOException;
	
	/**
	 * Register a standing interest filter with callback to receive any 
	 * matching interests seen
	 */
	public void setInterestFilter(ContentName filter, CCNFilterListener callbackListener);
	
	/**
	 * Unregister a standing interest filter
	 */
	public void cancelInterestFilter(ContentName filter, CCNFilterListener callbackListener);
	
	public ArrayList<CompleteName> enumerate(Interest interest, long timeout) throws IOException;

	/**
	 * High-level verify. Calls low-level verify, if we
	 * don't think this has been verified already. Probably
	 * need to separate to keep the two apart.
	 * @param object
	 * @param publicKey The key to use to verify the signature,
	 * 	or null if the key should be retrieved using the key 
	 *  locator.
	 * @return
	 * @throws XMLStreamException 
	 * @throws NoSuchAlgorithmException 
	 * @throws SignatureException 
	 * @throws InvalidKeyException 
	 */
	public boolean verify(ContentObject object, PublicKey publicKey) 
			throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, XMLStreamException;
	
	/**
	 * Medium level interface for retrieving pieces of a file
	 */
	public ContentObject getNext(ContentName name, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getNext(ContentName name, ExcludeFilter omissions, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getNext(ContentObject content, int prefixCount, long timeout)
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getLatest(ContentName name, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getLatest(ContentObject content, int prefixCount, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getLatest(ContentName name, ExcludeFilter omissions, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	public ContentObject getExcept(ContentName name, ExcludeFilter omissions, long timeout) 
					throws MalformedContentNameStringException, IOException, InterruptedException, InvalidParameterException;
	
	/**
	 * Approaches to read and write content. Low-level CCNBase returns
	 * a specific piece of content from the repository (e.g.
	 * if you ask for a fragment, you get a fragment). Library
	 * customers want the actual content, independent of
	 * fragmentation. Can implement this in a variety of ways;
	 * could verify fragments and reconstruct whole content
	 * and return it all at once. Could (better) implement
	 * file-like API -- open opens the header for a piece of
	 * content, read verifies the necessary fragments to return
	 * that much data and reads the corresponding content.
	 * Open read/write or append does?
	 * 
	 * DKS: TODO -- state-based put() analogous to write()s in
	 * blocks; also state-based read() that verifies. Start
	 * with state-based read.
	 */
	
	/**
	 * Beginnings of file system interface. If name is not versioned,
	 * for read, finds the latest version meeting the constraints.
	 * For writes, probably also should figure out the next version
	 * and open that for writing. Might get more complicated later;
	 * a file system (e.g. FUSE) layer on top of this might get more
	 * complicated still (e.g. mechanisms for detecting what the latest
	 * version is to make a new one for writing right now can't detect
	 * that we're already in the process of writing a given version).
	 * For now, we constraint the types of open modes we know about.
	 * We can't really append to an existing file, so we really can
	 * only pretty much open for writing or reading.
	 * @return a CCNDescriptor, which contains, among other things,
	 * the actual name we are opening. It also contains things
	 * like offsets and verification information.
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws XMLStreamException 
	 */
	public CCNDescriptor open(CompleteName name, OpenMode mode) throws IOException, InterruptedException, XMLStreamException;
	
	public long read(CCNDescriptor ccnObject, byte [] buf, long offset, long len) throws IOException, InterruptedException;

	public long write(CCNDescriptor ccnObject, byte [] buf, long offset, long len) throws IOException, InterruptedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException;
	
	public int seek(CCNDescriptor ccnObject, long offset, CCNDescriptor.SeekWhence whence) throws IOException, InterruptedException;
	
	public long tell(CCNDescriptor ccnObject);
	
	public int close(CCNDescriptor ccnObject) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InterruptedException, IOException;
	
	public void sync(CCNDescriptor ccnObject) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InterruptedException, IOException;
	
	/**
	 * Does this name refer to a node that represents
	 * local (protected) content?
	 * @param name
	 * @return
	 */
	public boolean isLocal(CompleteName name);
}
