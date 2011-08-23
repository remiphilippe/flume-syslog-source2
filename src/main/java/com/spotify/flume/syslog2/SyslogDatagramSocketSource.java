package com.spotify.flume.syslog2;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import com.cloudera.flume.core.Event;


/**
 * A Flume event source backed by a datagram socket and a syslog parser.
 *
 * Currently a new parser is created for every packet.
**/
public class SyslogDatagramSocketSource extends BaseSource {
	private SocketAddress addr;
	private DatagramSocket socket;
	private int bufferSize;

	/**
	 * Construct a new source.
	 *
	 * @param addr the address to bind to.
	 * @param bufferSize the maximum number of bytes per UDP message.
	**/
	public SyslogDatagramSocketSource(SocketAddress addr, int bufferSize) {
		this.addr = addr;
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void open() throws IOException {
		socket = createDatagramSocket();
	}

	/**
	 * Create a new datagram socket suitable for receiving packets on.
	**/
	protected DatagramSocket createDatagramSocket() throws IOException {
		return new DatagramSocket(addr);
	}
	
	@Override
	public Event next() throws IOException {
		byte[] buf = new byte[bufferSize];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		for (;;) {
			// IOExceptions from here should not be counted as
			// rejected.
			socket.receive(packet);
			
			try {
				Event e = new SyslogParser(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())).readEvent();
				
				updateEventProcessingStats(e);

				return e;
			} catch (IOException ex) {
				addRejectedMessage();
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
		socket = null;
	}
}
