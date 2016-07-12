package com.github.joshuasrjc.functionfighters.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.github.joshuasrjc.functionfighters.ui.ChatLog;

public class Packet
{	
	public static final int INFO = 0x00;
	public static final int ERROR = 0x01;
	public static final int CHAT = 0x02;
	public static final int CODE = 0x03;
	
	public static final int LOGIN = 0x10;
	
	public static final int ACCEPTED = 0x20;
	public static final int DENIED = 0x21;
	public static final int BAD_PASSWORD = 0x22;
	public static final int BAD_NICKNAME = 0x23;
	
	public static final int FRAME = 0x30;
	public static final int SCRIPT = 0x31;
	public static final int GAME_START = 0x32;
	public static final int GAME_PAUSE = 0x33;
	public static final int GAME_STOP = 0x34;
	
	public static final int ITEM_SELECT = 0x40;
	public static final int ITEM_ADD = 0x41;
	public static final int ITEM_REMOVE = 0x42;
	
	public static final String ENCODING = "UTF-8";
	
	public int type;
	public byte[] data;
	private String message = null;
	
	public Packet(int type)
	{
		this.type = type;
		this.data = new byte[0];
	}
	
	public Packet(int type, byte[] data)
	{
		this.type = type;
		this.data = data;
	}
	
	public Packet(int type, String message)
	{
		this.type = type;
		try
		{
			this.data = message.getBytes(ENCODING);
		}
		catch(UnsupportedEncodingException ex)
		{
			ChatLog.logError("Unable to encode string.");
			ex.printStackTrace();
		}
		
		System.out.print("[ ");
		for(byte b : data)
		{
			System.out.print(b + " ");
		}
		System.out.print("]\n");
	}
	
	public Packet(int type, int id)
	{
		this.type = type;
		this.data = new byte[4];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.putInt(id);
	}
	
	public Packet(int type, int id, int team)
	{
		this.type = type;
		this.data = new byte[8];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.putInt(id);
		buffer.putInt(team);
	}
	
	private void parseText()
	{
		try
		{
			message = new String(data, ENCODING);
		}
		catch(UnsupportedEncodingException ex)
		{
			ChatLog.logError("Unable to decode string.");
			ex.printStackTrace();
		}
	}
	
	public String getMessage()
	{
		if(message == null) parseText();
		return message;
	}
	
	public String getFirstLine()
	{
		System.out.print("[ ");
		for(byte b : data)
		{
			System.out.print(b + " ");
		}
		System.out.print("]\n");
		
		if(message == null) parseText();
		return message.substring(0, message.indexOf('\n'));
	}
	
	public String getAllButFirstLine()
	{
		if(message == null) parseText();
		return message.substring(message.indexOf('\n') + 1);
	}
	
	public boolean isMessage()
	{
		return type < 0x10;
	}
	
	public int getIndex()
	{
		ByteBuffer buffer = ByteBuffer.wrap(data);
		return buffer.getInt();
	}
	
	public int getTeam()
	{
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.getInt();
		return buffer.getInt();
	}
}
