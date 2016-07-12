package com.github.joshuasrjc.functionfighters.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.github.joshuasrjc.functionfighters.game.Bullet;
import com.github.joshuasrjc.functionfighters.game.Fighter;
import com.github.joshuasrjc.functionfighters.game.GameObject;

public class Frame
{
	public GameObject[] objects;
	public byte[] bytes;
	
	public Frame(Packet packet)
	{
		fromBytes(packet.data);
	}
	
	public Frame(byte[] bytes)
	{
		fromBytes(bytes);
	}
	
	public Frame(GameObject[] objects)
	{
		int size = 0;
		
		for(int i = 0; i < objects.length; i++)
		{
			size += 1 + objects[i].getByteSize();
		}
		
		ByteBuffer data = ByteBuffer.allocate(size);

		for(int i = 0; i < objects.length; i++)
		{
			GameObject obj = objects[i];
			
			if(obj.getClass().equals(GameObject.class))
			{
				data.put(GameObject.OBJECT_ID);
			}
			else if(obj.getClass().equals(Fighter.class))
			{
				data.put(GameObject.FIGHTER_ID);
			}
			else if(obj.getClass().equals(Bullet.class))
			{
				data.put(GameObject.BULLET_ID);
			}
			
			objects[i].toByteBuffer(data);
		}
		
		this.objects = objects;
		this.bytes = data.array();
	}
	
	private void fromBytes(byte[] bytes)
	{
		ByteBuffer data = ByteBuffer.wrap(bytes);
		ArrayList<GameObject> objects = new ArrayList<GameObject>();
		
		while(data.hasRemaining())
		{
			byte objID = data.get();
			GameObject obj;
			
			switch(objID)
			{
			case GameObject.OBJECT_ID:
				obj = new GameObject(data);
				break;
				
			case GameObject.FIGHTER_ID:
				obj = new Fighter(data);
				break;
				
			case GameObject.BULLET_ID:
				obj = new Bullet(data);
				break;
			
			default:
				obj = null;
			}
			
			objects.add(obj);
		}
		
		this.objects = objects.toArray(new GameObject[objects.size()]);
	}
	
	public Packet toPacket()
	{
		return new Packet(Packet.FRAME, bytes);
	}
}
