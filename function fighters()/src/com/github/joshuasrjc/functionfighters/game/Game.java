package com.github.joshuasrjc.functionfighters.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.luaj.vm2.LuaValue;

import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.ui.GameViewer;

public class Game implements Runnable
{
	public LuaValue toLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		lv.set("top", TOP);
		lv.set("bottom", BOTTOM);
		lv.set("left", LEFT);
		lv.set("right", RIGHT);
		lv.set("nTeams", N_TEAMS);
		return lv;
	}
	
	public static final int N_TEAMS = 2;
	public static final long FRAME_MILLIS = 20;
	
	public static final float TOP = -300;
	public static final float BOTTOM = 300;
	public static final float LEFT = -400;
	public static final float RIGHT = 400;
	
	private boolean gameRunning = false;
	private boolean gamePaused = false;
	private boolean threadRunning = true;
	
	public static final float G = 0.0f;
	
	private Thread thread;
	
	public ArrayList<GameObject> objects = new ArrayList<GameObject>();
	
	int frameIndex = 0;
	
	Server server;
	GameViewer viewer;
	
	public Game(Server server)
	{
		this.server = server;
	}
	
	public boolean start()
	{
		String[] scripts = server.getScripts();
		if(scripts == null)
		{
			server.sendMessageToAllClients(Server.ERROR, "Must select a script for each team.");
			return false;
		}
		
		for(int i = 0; i < scripts.length; i++)
		{
			Self fighter = new Self(this, Vector2.randomInCircle(300), scripts[i], server);
			objects.add(fighter);
		}
		
		gameRunning = true;
		
		thread = new Thread(this);
		thread.start();
		
		return true;
	}
	
	public void pause()
	{
		gamePaused = true;
	}
	
	public void resume()
	{
		gamePaused = false;
	}
	
	public void stop()
	{
		thread = null;
		gameRunning = false;
		gamePaused = false;
		
		while(objects.size() > 0)
		{
			objects.get(0).destroy();
		}
	}
	
	public void preUpdate()
	{
		for(GameObject obj : objects)
		{
			obj.preUpdate();
		}
	}
	
	public void update()
	{
		for(GameObject obj : objects)
		{
			obj.update();
		}
	}
	
	public void postUpdate()
	{
		for(int i = 0; i < objects.size(); i++)
		{
			GameObject obj = objects.get(i);
			obj.postUpdate();
		}
	}
	
	public void sendFrame()
	{
		GameObject[] objectArray = objects.toArray(new GameObject[objects.size()]);
		Frame frame = new Frame(frameIndex, objectArray);
		server.sendFrameToAllClients(frame);
		frameIndex++;
		frameIndex %= 256;
	}
	
	@Override
	public void run()
	{
		while(gameRunning)
		{
			preUpdate();
			update();
			postUpdate();
			sendFrame();
			try { Thread.sleep(FRAME_MILLIS); } catch(Exception ex) { break; }
			
			while(gameRunning && gamePaused)
			{
				try { Thread.sleep(FRAME_MILLIS); } catch(Exception ex) { break; }
			}
		}
	}
	
	public boolean isRunning()
	{
		return gameRunning;
	}
	
	public boolean isPaused()
	{
		return gamePaused;
	}
}
