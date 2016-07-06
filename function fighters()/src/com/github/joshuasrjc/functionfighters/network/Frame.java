package com.github.joshuasrjc.functionfighters.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.github.joshuasrjc.functionfighters.game.GameObject;

public class Frame
{
	public int index;
	public GameObject[] objects;
	public byte[] bytes;
	
	Frame(int index, byte[] bytes)
	{
		this.index = index;
		
		int nObjects = bytes.length / GameObject.BYTE_SIZE;
		objects = new GameObject[nObjects];
		
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		for(int i = 0; i < objects.length; i++)
		{
			objects[i] = GameObject.fromByteBuffer(null, buffer);
		}
		
		this.bytes = bytes;
	}
	
	public Frame(int index, GameObject[] objects)
	{
		this.index = index;
		
		int nBytes = objects.length * GameObject.BYTE_SIZE;
		
		bytes = new byte[nBytes];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		for(int i = 0; i < objects.length; i++)
		{
			objects[i].toByteBuffer(buffer);
		}
		
		this.objects = objects;
	}
}
