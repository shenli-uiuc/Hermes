/**
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
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
package org.ccnx.ccn.impl;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.ccnx.ccn.impl.CCNNetworkManager.NetworkProtocol;
import org.ccnx.ccn.impl.encoding.XMLEncodable;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.content.ContentDecodingException;
import org.ccnx.ccn.protocol.WirePacket;

/**
 *  This guy manages all of the access to the network connection.
 *  We do this as a separate class so we can support both TCP and UDP
 *  transports.
 */
public class CCNNetworkChannel {
	public static final int MAX_PAYLOAD = 8800; // number of bytes in UDP payload
	public static final int HEARTBEAT_PERIOD = 3500;
	public static final int SOCKET_TIMEOUT = 1000; // period to wait in ms.

	protected String _ncHost;
	protected int _ncPort;
	protected NetworkProtocol _ncProto;
	protected int _ncLocalPort;
	protected DatagramChannel _ncDGrmChannel = null;
	protected SocketChannel _ncSockChannel = null;
	protected Selector _ncSelector = null;
	protected Boolean _ncConnected = new Boolean(false);
	protected boolean _ncInitialized = false;
	protected Timer _ncHeartBeatTimer = null;
	protected Boolean _ncStarted = false;
	protected InputStream _ncStream = null;
	protected FileOutputStream _ncTapStreamIn = null;
	protected boolean _run = true;
	
	// Allocate datagram buffer
	protected ByteBuffer _datagram = ByteBuffer.allocateDirect(MAX_PAYLOAD);
	
	public CCNNetworkChannel(String host, int port, NetworkProtocol proto, FileOutputStream tapStreamIn) throws IOException {
		_ncHost = host;
		_ncPort = port;
		_ncProto = proto;
		_ncTapStreamIn = tapStreamIn;
		_ncSelector = Selector.open();
<<<<<<< HEAD
		_ncStream = new CCNInputStream(this);
=======
		Log.info("Starting up CCNNetworkChannel using {0}.", proto);
>>>>>>> 9410d45... New TCP basically works
	}
	
	/**
	 * Open the channel to ccnd depending on the protocol, connect on the ccnd port and
	 * set up the selector
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
		if (_ncProto == NetworkProtocol.UDP) {
			try {
				_ncDGrmChannel = DatagramChannel.open();
				_ncDGrmChannel.connect(new InetSocketAddress(_ncHost, _ncPort));
				_ncDGrmChannel.configureBlocking(true);
				ByteBuffer test = ByteBuffer.allocate(1);
				int ret = _ncDGrmChannel.write(test);
				if (ret < 1)
					return;
				wakeup();
				_ncDGrmChannel.register(_ncSelector, SelectionKey.OP_READ);
				_ncLocalPort = _ncDGrmChannel.socket().getLocalPort();
				if (_ncStarted)
					_ncHeartBeatTimer.schedule(new HeartBeatTimer(), 0);
				_ncConnected = true;
			} catch (IOException ioe) {
				return;
			}
		} else if (_ncProto == NetworkProtocol.TCP) {
			_ncSockChannel = SocketChannel.open();
			_ncSockChannel.connect(new InetSocketAddress(_ncHost, _ncPort));
			_ncSockChannel.configureBlocking(false);
			_ncSockChannel.register(_ncSelector, SelectionKey.OP_READ);			
		} else {
			throw new IOException("NetworkChannel: invalid protocol specified");
		}
		String connecting = (_ncInitialized ? "Reconnecting to" : "Contacting");
		Log.info(connecting + " CCN agent at " + _ncHost + ":" + _ncPort + " on local port " + _ncLocalPort);
		_ncStream = new CCNInputStream(this);
		clearSelectedKeys();
		_ncInitialized = true;
	}
	
	public XMLEncodable getPacket() throws ContentDecodingException {
		WirePacket packet = new WirePacket();
		packet.decode(_ncStream);
		return packet.getPacket();
	}
	
	/**
	 * Close the channel depending on the protocol
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (_ncProto == NetworkProtocol.UDP) {
			_ncConnected = false;
			wakeup();
			_ncDGrmChannel.close();
		} else if (_ncProto == NetworkProtocol.TCP) {
			_ncSockChannel.close();
		} else {
			throw new IOException("NetworkChannel: invalid protocol specified");
		}
	}
	
	/**
	 * Check whether the channel is currently connected.  This is really a test
	 * to see whether ccnd is running. If it isn't the channel is not connected.
	 * @return true if connected
	 */
	public boolean isConnected() {
		if (_ncProto == NetworkProtocol.UDP) {
			return _ncConnected;
		} else if (_ncProto == NetworkProtocol.TCP) {
			return (_ncSockChannel.isConnected());
		} else {
			Log.severe("NetworkChannel: invalid protocol specified");
			return false;
		}
	}
	
	/**
	 * Return an InputStream to read data from the ccnd channel depending on the protocol.
	 * The stream is directly passed to the decoder. For UDP we just convert the data into
	 * a ByteArrayInputStream which is what we always have done. For TCP, we may need to do
	 * multiple reads from the channel so we handle that by creating a custom input stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		clearSelectedKeys();
		_datagram.clear(); // make ready for new read
		//_ncStream.init();
		//_ncStream.fill();
		return _ncStream;
	}
    
	/**
	 * Write to ccnd using methods based on the protocol type
	 * @param src - ByteBuffer to write
	 * @return - number of bytes written
	 * @throws IOException
	 */
	public int write(ByteBuffer src) throws IOException {
		Log.finest("NetworkChannel.write() on port " + _ncLocalPort);
		if (_ncProto == NetworkProtocol.UDP) {
			return (_ncDGrmChannel.write(src));
		} else if (_ncProto == NetworkProtocol.TCP) {
			return (_ncSockChannel.write(src));
		} else {
			throw new IOException("NetworkChannel: invalid protocol specified");
		}
	}
	
	private void clearSelectedKeys() {
		_ncSelector.selectedKeys().clear();
	}

	/**
	 * Perform a select based on incoming ccnd data
	 * @param timeout in ms
	 * @return number of channels selected - in practice this will always be 0 or 1
	 * @throws IOException
	 */
	public int select(long timeout) throws IOException {
		int selectVal = (_ncSelector.select(timeout));
		return selectVal;
	}
	
	/**
	 * Force wakeup from a select
	 * @return the selector
	 */
	public Selector wakeup() {
		return (_ncSelector.wakeup());
	}
	
	/**
	 * Initialize the channel at the point when we are actually ready to create faces
	 * with ccnd
	 */
	public void init() {
		if (_ncProto == NetworkProtocol.UDP) {
			if (! _ncStarted) {
				_ncHeartBeatTimer = new Timer(true);
				_ncHeartBeatTimer.schedule(new HeartBeatTimer(), 0L);
				_ncStarted = true;
			}
		}
	}
	
	public void shutdown() {
		_run = false;
	}
	
	/**
	 * Create an input stream to allow multiple reads from the TCP channel
	 * when necessary. The algorithm tries to use only the single current buffer called _datagram
	 * which is also used to implement the UDP algorithm.
	 */
	private class CCNInputStream extends InputStream {
		private CCNNetworkChannel _channel;
		private int _mark = 0;
		private int _readLimit = 0;
		
		private CCNInputStream(CCNNetworkChannel channel) {
			_channel = channel;
			_datagram.clear(); // make ready for new read
			_datagram.limit(0);
			_mark = 0;
			_readLimit = 0;
		}
		
		@Override
		public int read() throws IOException {
			while (true) {
				try {
					if (_datagram.hasRemaining()) {
						int ret = (int)_datagram.get();
						return ret & 0xff;
					}
				} catch (BufferUnderflowException bfe) {}
				int ret = fill();
				if (ret <= 0)
					return ret;
			}
		}
		
		@Override
		public boolean markSupported() {
			return true;
		}
		
		@Override
		public void mark(int readlimit) {
			_readLimit = readlimit;
			_mark = _datagram.position();
		}
		
		@Override
		public void reset() throws IOException {
			_datagram.position(_mark);
		}
		
		/**
		 * Refill the buffer. We don't reset the start of it unless necessary (i.e. we have
		 * reached the end of the buffer). If the start is reset and a mark has been set within
		 * "readLimit" bytes of the end, we need to copy the end of the previous buffer out
		 * to the start so that a reset is possible.
		 * 
		 * @return
		 * @throws IOException
		 */
		private int fill() throws IOException {
			int ret;
			int position = _datagram.position();
			if (position >= _datagram.capacity()) {
				byte[] b = null;
				boolean doCopy = false;
				int checkPosition = position - 1;
				doCopy = _mark + _readLimit >= checkPosition && _mark <= checkPosition;
				if (doCopy) {
					b = new byte[checkPosition - (_mark - 1)];
					_datagram.position(_mark);
					_datagram.get(b);
				}
				_datagram.clear();
				if (doCopy) {
					Log.info("Copy of " + b.length + " bytes");
					_datagram.put(b);
				}
				_mark = 0;
				position = _datagram.position();
			}
			_datagram.limit(_datagram.capacity());
			if (_ncProto == NetworkProtocol.UDP) {
				synchronized (_channel) {
					ret = _ncDGrmChannel.read(_datagram);
				}
			} else {
				ret = 0;
				if (select(SOCKET_TIMEOUT) != 0) {
					ret = _ncSockChannel.read(_datagram);
				}
				clearSelectedKeys();
			}
			_datagram.position(position);
			_datagram.limit(position + ret);
			if (null != _ncTapStreamIn) {
				byte [] b = new byte[ret];
				_datagram.get(b);
				_ncTapStreamIn.write(b);
				_datagram.position(position);
				_datagram.limit(position + ret);
			}
			return ret;
		}
	}
			
	/**
	 * Do scheduled writes of heartbeats on UDP connections.
	 */
	private class HeartBeatTimer extends TimerTask {
		public void run() {
			if (_ncConnected) {
				try {
					ByteBuffer heartbeat = ByteBuffer.allocate(1);
					_ncDGrmChannel.write(heartbeat);
					_ncHeartBeatTimer.schedule(new HeartBeatTimer(), HEARTBEAT_PERIOD);
				} catch (IOException io) {
					// We do not see errors on send typically even if 
					// agent is gone, so log each but do not track
					Log.warning("Error sending heartbeat packet: {0}", io.getMessage());
					try {
						close();
					} catch (IOException e) {}
				}
			}
		} /* run() */	
	} /* private class HeartBeatTimer extends TimerTask */
} /* NetworkChannel */