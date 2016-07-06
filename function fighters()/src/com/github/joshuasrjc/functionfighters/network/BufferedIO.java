package com.github.joshuasrjc.functionfighters.network;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

public class BufferedIO
{
	public static String byteToString(byte b)
	{
		byte r = (byte)(0x0F & b);
		if(r < 0xA) r += '0';
		else r += 'A' - 0xA;
		
		b >>= 4;
		
		byte l = (byte)(0x0F & b);
		if(l < 0xA) l += '0';
		else l += 'A' - 0xA;
		
		return "" + (char)l + (char)r;
	}
	
	public static String bytesToString(byte[] bytes)
	{
		String str = "";
		for(byte b : bytes)
		{
			str += byteToString(b) + " ";
		}
		return str;
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
	
	public void flush() throws IOException
	{
		out.flush();
	}
	
	public void writeByte(byte b) throws IOException
	{
		out.write(b);
	}
	
	public void writeBytes(byte[] bytes) throws IOException
	{
		out.write(bytes);
	}
	
	public void writeBytes(ByteBuffer buffer) throws IOException
	{
		writeBytes(buffer.array());
	}
	
	public void writeString(String str) throws IOException
	{
		out.write(str.getBytes(), 0, str.length());
	}
	
	public void newLine() throws IOException
	{
		out.write('\n');
	}
	
	public byte readByte() throws IOException
	{
		int i = in.read();
		if(i < 0) throw new IOException();
		
		return (byte)i;
	}
	
	public byte[] readBytes(int nBytes) throws IOException
	{
		byte[] bytes = new byte[nBytes];
		int read = 0;
		while(read < bytes.length)
		{
			read += in.read(bytes, read, bytes.length - read);
		}
		
		return bytes;
	}
	
	public String readLine() throws IOException
	{
		String str = "";
		for(int i = in.read(); i != '\n'; i = in.read())
		{
			if(i < 0) throw new IOException();
			str += (char)i;
		}
		
		return str;
	}
}
