/**
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.ccnx.ccn.io;

import java.io.IOException;
import java.io.InputStream;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.impl.security.crypto.ContentKeys;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.access.AccessControlManager;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;


/**
 * Perform sequential reads on any segmented CCN content, as if it
 * were a standard {@link InputStream}.
 * This input stream will read from a sequence of blocks, authenticating
 * each as it goes, and caching what verification information it can to speed
 * up verification of future blocks. All it assumes
 * is that the last component of the name is a segment number as described in
 * {@link SegmentationProfile}.
 * 
 * Read buffer size is independent of segment size; the stream will pull additional
 * content fragments dynamically when possible to fill out the requested number
 * of bytes.
 * @author smetters
 */
public class CCNInputStream extends CCNAbstractInputStream {
	
	/**
	 * Set up an input stream to read segmented CCN content under a given name. Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}). 
	 * Will use the default handle given by {@link CCNHandle.getHandle()}.
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name) throws IOException {
		this(name, null);
	}
	
	/**
	 * Set up an input stream to read segmented CCN content under a given name. Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}).
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name, CCNHandle handle) throws IOException {
		this(name, null, null, handle);
	}
	
	/**
	 * Set up an input stream to read segmented CCN content under a given name. Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}).
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param publisher The key we require to have signed this content. If null, will accept any publisher
	 * 				(subject to higher-level verification).
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name, PublisherPublicKeyDigest publisher, CCNHandle handle) 
			throws IOException {
		this(name, null, publisher, handle);
	}

	/**
	 * Set up an input stream to read segmented CCN content under a given name. Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}).
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param startingSegmentNumber Alternative specification of starting segment number. If
	 * 		null, will be {@link SegmentationProfile.baseSegment()}.
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name, Long startingSegmentNumber, CCNHandle handle) throws IOException {
		this(name, startingSegmentNumber, null, handle);
	}
	
	/**
	 * Set up an input stream to read segmented CCN content under a given name. Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}).
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param startingSegmentNumber Alternative specification of starting segment number. If
	 * 		null, will be {@link SegmentationProfile.baseSegment()}.
	 * @param publisher The key we require to have signed this content. If null, will accept any publisher
	 * 				(subject to higher-level verification).
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name, Long startingSegmentNumber, PublisherPublicKeyDigest publisher,
			CCNHandle handle) throws IOException {

		super(name, startingSegmentNumber, publisher, null, handle);
	}
	
	/**
	 * Set up an input stream to read segmented CCN content under a given name. 
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param startingSegmentNumber Alternative specification of starting segment number. If
	 * 		null, will be {@link SegmentationProfile.baseSegment()}.
	 * @param publisher The key we require to have signed this content. If null, will accept any publisher
	 * 				(subject to higher-level verification).
	 * @param keys The keys to use to decrypt this content. If null, assumes content unencrypted, or another
	 * 				process will be used to retrieve the keys (for example, an {@link AccessControlManager}).
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
	public CCNInputStream(ContentName name, Long startingSegmentNumber, PublisherPublicKeyDigest publisher, 
			ContentKeys keys, CCNHandle handle) throws IOException {

		super(name, startingSegmentNumber, publisher, keys, handle);
	}

	/**
	 * Set up an input stream to read segmented CCN content starting with a given
	 * {@link ContentObject} that has already been retrieved.  Content is assumed
	 * to be unencrypted, or keys will be retrieved automatically via another
	 * process (for example, an {@link AccessControlManager}).
	 * @param startingSegment The first segment to read from. If this is not the
	 * 		first segment of the stream, reading will begin from this point.
	 * 		We assume that the signature on this segment was verified by our caller.
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException
	 */
	public CCNInputStream(ContentObject startingSegment, CCNHandle handle) throws IOException {
		super(startingSegment, null, handle);
	}
	
	/**
	 * Set up an input stream to read segmented CCN content starting with a given
	 * {@link ContentObject} that has already been retrieved.  
	 * @param startingSegment The first segment to read from. If this is not the
	 * 		first segment of the stream, reading will begin from this point.
	 * 		We assume that the signature on this segment was verified by our caller.
	 * @param keys The keys to use to decrypt this content. Null if content unencrypted, or another
	 * 				process will be used to retrieve the keys (for example, an {@link AccessControlManager}).
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by {@link CCNHandle.getHandle()} will be used.
	 * @throws IOException
	 */
	public CCNInputStream(ContentObject startingSegment, ContentKeys keys, CCNHandle handle) throws IOException {
		super(startingSegment, keys, handle);
	}
	
	/**
	 * Implement sequential reads of data across multiple segments. As we run out of bytes
	 * on a given segment, the next segment is retrieved and reading continues.
	 */
	@Override
	protected int readInternal(byte [] buf, int offset, int len) throws IOException {
		
		if (_atEOF) {
			return -1;
		}
		
		Log.finest(getBaseName() + ": reading " + len + " bytes into buffer of length " + 
				((null != buf) ? buf.length : "null") + " at offset " + offset);
		// is this the first block?
		if (null == _currentSegment) {
			// This will throw an exception if no block found, which is what we want.
			setFirstSegment(getFirstSegment());
		} 
		Log.finest("reading from block: {0}, length: {1}", _currentSegment.name(),  
				_currentSegment.contentLength());
		
		// Now we have a block in place. Read from it. If we run out of block before
		// we've read len bytes, pull next block.
		int lenToRead = len;
		int lenRead = 0;
		long readCount = 0;
		while (lenToRead > 0) {
			if (null == _segmentReadStream) {
				Log.severe("Unexpected null block read stream!");
			}
			if (null != buf) {  // use for skip
				Log.finest("before block read: content length "+_currentSegment.contentLength()+" position "+ tell() +" available: " + _segmentReadStream.available() + " dst length "+buf.length+" dst index "+offset+" len to read "+lenToRead);
				// Read as many bytes as we can
				readCount = _segmentReadStream.read(buf, offset, lenToRead);
			} else {
				readCount = _segmentReadStream.skip(lenToRead);
			}

			if (readCount <= 0) {
				Log.info("Tried to read at end of block, go get next block.");
				if (!hasNextSegment()) {
					Log.info("No next block expected, setting _atEOF, returning " + ((lenRead > 0) ? lenRead : -1));
					_atEOF = true;
					if (lenRead > 0) {
						return lenRead;
					}
					return -1; // no bytes read, at eof					
				}
				ContentObject nextSegment = getNextSegment();
				if (null == nextSegment) {
					Log.info("Next block is null, setting _atEOF, returning " + ((lenRead > 0) ? lenRead : -1));
					_atEOF = true;
					if (lenRead > 0) {
						return lenRead;
					}
					return -1; // no bytes read, at eof
				}
				setCurrentSegment(nextSegment);

				Log.info("now reading from block: " + _currentSegment.name() + " length: " + 
						_currentSegment.contentLength());
			} else {
				offset += readCount;
				lenToRead -= readCount;
				lenRead += readCount;
				Log.finest("     read " + readCount + " bytes for " + lenRead + " total, " + lenToRead + " remaining.");
			}
		}
		return lenRead;
	}
}

