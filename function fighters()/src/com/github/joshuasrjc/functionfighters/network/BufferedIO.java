package com.github.joshuasrjc.functionfighters.network;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

public class BufferedIO
{	
	private static int unsignByte(byte b)
	{
		return (int)(b & 0xFF);
	}
	
	private static byte signByte(int i)
	{
		return (byte)(i);
	}
	
	private BufferedInputStream in;
	private BufferedOutputStream out;
	
	BufferedIO(Socket socket) throws IOException
	{
		in = new BufferedInputStream(socket.getInputStream());
		out = new BufferedOutputStream(socket.getOutputStream());
	}
	
	public void close() throws IOException
	{
		in.close();
		out.close();
	}
	
	public void writePacket(Packet packet) throws IOException
	{
		writeByte(signByte(packet.type));
		writeShort((short)packet.data.length);
		writeBytes(packet.data);
		out.flush();
	}
	
	public Packet readPacket() throws IOException
	{
		int type = unsignByte(readByte());
		int size = readShort();
		byte[] data = new byte[0];
		if(size > 0)
		{
			data = readBytes(size);
		}
		return new Packet(type, data);
	}
	
	private void writeByte(byte b) throws IOException
	{
		out.write(b);
	}
	
	private void writeShort(short s) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(s);
		buffer.rewind();
		writeByte(buffer.get());
		writeByte(buffer.get());
	}
	
	private void writeBytes(byte[] bytes) throws IOException
	{
		out.write(bytes);
	}
	
	private byte readByte() throws IOException
	{
		int i = in.read();
		if(i < 0) throw new IOException();
		
		return (byte)i;
	}
	
	private short readShort() throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.put(readByte());
		buffer.put(readByte());
		buffer.rewind();
		return buffer.getShort();
	}
	
	private byte[] readBytes(int nBytes) throws IOException
	{
		byte[] bytes = new byte[nBytes];
		int read = 0;
		while(read < bytes.length)
		{
			read += in.read(bytes, read, bytes.length - read);
		}
		
		return bytes;
	}
}
