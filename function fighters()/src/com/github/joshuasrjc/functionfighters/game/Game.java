package com.github.joshuasrjc.functionfighters.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import com.github.joshuasrjc.functionfighters.network.Client;
import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.network.ServerListener;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;
import com.github.joshuasrjc.functionfighters.ui.GameViewer;

public class Game implements Runnable, ServerListener
{
	public LuaValue toLuaValue(Fighter fighter)
	{
		GameFunctions gf = new GameFunctions(fighter);
		
		LuaValue lv = LuaValue.tableOf();
		lv.set("top", TOP);
		lv.set("bottom", BOTTOM);
		lv.set("left", LEFT);
		lv.set("right", RIGHT);
		lv.set("nTeams", N_TEAMS);
		lv.set("nFightersPerTeam", N_FIGHTERS_PER_TEAM);
		lv.set("time", time);
		lv.set("findWeakestEnemy", gf.findWeakestEnemy);
		return lv;
	}
	
	public static final int N_TEAMS = 2;
	public static final int N_FIGHTERS_PER_TEAM = 1;
	public static final long FRAME_MILLIS = 20;
	
	public static final float TOP = -300;
	public static final float BOTTOM = 300;
	public static final float LEFT = -600;
	public static final float RIGHT = 600;
	
	private Game game = this;
	private Server server;
	private ArrayList<String> scriptNames = new ArrayList<String>();
	private ArrayList<String> scripts = new ArrayList<String>();
	private int[] selections = new int[N_TEAMS];
	
	private boolean gameRunning = false;
	private boolean gamePaused = false;
	private boolean threadRunning = true;
	private long time = 0;
	
	public static final float G = 0.0f;
	
	private Thread thread;
	
	public ArrayList<GameObject> objects = new ArrayList<GameObject>();
	public Fighter[][] fighters = new Fighter[N_TEAMS][N_FIGHTERS_PER_TEAM];
	public Vector2[][] positions = {
		{new Vector2(-400, 200)},
		{new Vector2(400, -200)}
	};
	
	int frameIndex = 0;
	
	
	public Game(Server server)
	{
		this.server = server;
		for(int i = 0; i < selections.length; i++)
		{
			selections[i] = -1;
		}
	}
	
	public void addScript(String name, String text)
	{
		scriptNames.add(name);
		scripts.add(text);
	}

	@Override
	public void onClientConnected(Client client)
	{
		for(int i = 0; i < scriptNames.size(); i++)
		{
			client.sendPacket(new Packet(Packet.ITEM_ADD, scriptNames.get(i)));
		}
		for(int i = 0; i < N_TEAMS; i++)
		{
			if(selections[i] >= 0)
			{
				client.sendPacket(new Packet(Packet.ITEM_SELECT, selections[i], i));
			}
		}
	}
	
	@Override
	public void onClientSentPacket(Client client, Packet packet)
	{
		int type = packet.type;
		if(type == Packet.GAME_START)
		{
			if(!game.isRunning())
			{
				if(game.start()) server.sendPacketToAllClients(new Packet(Packet.GAME_START));
			}
			else if(game.isPaused())
			{
				game.resume();
				server.sendPacketToAllClients(new Packet(Packet.GAME_START));
			}
			else
			{
				client.sendPacket(new Packet(Packet.ERROR, "A game is already running."));
			}
		}
		else if(type == Packet.GAME_PAUSE)
		{
			if(game.isRunning())
			{
				if(!game.isPaused())
				{
					game.pause();
					server.sendPacketToAllClients(new Packet(Packet.GAME_PAUSE));
				}
				else
				{
					client.sendPacket(new Packet(Packet.ERROR, "The game is already paused."));
				}
			}
			else
			{
				client.sendPacket(new Packet(Packet.ERROR, "No game is running."));
			}
		}
		else if(type == Packet.GAME_STOP)
		{
			if(game.isRunning())
			{
				game.stop();
				System.out.println("Game stop.");
				server.sendPacketToAllClients(new Packet(Packet.GAME_STOP));
			}
			else
			{
				client.sendPacket(new Packet(Packet.ERROR, "No game is running."));
			}
		}
		else if(type == Packet.SCRIPT)
		{
			String name = packet.getFirstLine();
			String text = packet.getAllButFirstLine();
			addScript(name, text);
			server.sendPacketToAllClients(new Packet(Packet.ITEM_ADD, name));
		}
		else if(type == Packet.ITEM_SELECT)
		{
			int index = packet.getIndex();
			int team = packet.getTeam();
			if(index >= 0 && index < scripts.size() && team >= 0 && team < N_TEAMS)
			{
				selections[team] = index;
				server.sendPacketToAllClients(new Packet(Packet.ITEM_SELECT, index, team));
				System.out.println("Selected" + team + ", " + index);
			}
		}
	}
	
	private String[] getScripts()
	{
		String[] scripts = new String[N_TEAMS];
		for(int i = 0; i < N_TEAMS; i++)
		{
			int index = selections[i];
			if(index < 0 || index >= this.scripts.size()) return null;
			scripts[i] = this.scripts.get(selections[i]);
		}
		return scripts;
	}
	
	public boolean start()
	{
		time = 0;
		String[] scripts = getScripts();
		if(scripts == null)
		{
			server.sendPacketToAllClients(new Packet(Packet.ERROR, "Must select a script for each team."));
			return false;
		}
		
		for(int team = 0; team < N_TEAMS; team++)
		{
			for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
			{
				Fighter fighter = new Fighter(this, positions[team][id], team, id, scripts[team], server);
				fighters[team][id] = fighter;
				objects.add(fighter);
			}
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
		for(int i = 0; i < objects.size(); i++)
		{
			GameObject obj = objects.get(i);
			obj.preUpdate();
		}
	}
	
	public void update()
	{
		for(int i = 0; i < objects.size(); i++)
		{
			GameObject obj = objects.get(i);
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
		Frame frame = new Frame(objectArray);
		server.sendPacketToAllClients(frame.toPacket());
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
			time++;
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
	
	private float getRayCastDistance(Vector2 start, Vector2 dir, GameObject obj)
	{
		Vector2 v1 = obj.position.minus(start);
		float dist = v1.getMagnitude();
		float radius = obj.getRadius();
		
		if(dist <= radius) return dist;
		
		v1.divide(dist);
		float cos = v1.dot(dir);
		
		if(cos <= 0f) return -1f;
		
		float hyp = new Vector2(dist, radius).getMagnitude();
		float minCos = dist / hyp;
		
		if(cos > minCos) return dist;
		
		return -1f;
	}
	
	public GameObject castRay(Vector2 start, Vector2 dir, RayCastFilter filter)
	{
		GameObject closestObject = null;
		float closestDistance = Float.MAX_VALUE;
		for(int i = 0; i < objects.size(); i++)
		{
			GameObject obj = objects.get(i);
			if(filter.doTest(obj))
			{
				float distance = getRayCastDistance(start, dir, obj);
				if(distance >= 0f && distance < closestDistance)
				{
					closestDistance = distance;
					closestObject = obj;
				}
			}
		}
		return closestObject;
	}

	private class GameFunctions
	{
		Fighter fighter;
		
		GameFunctions(Fighter fighter)
		{
			this.fighter = fighter;
		}
		
		public LuaValue getFighters = new ZeroArgFunction() { @Override public LuaValue call()
		{
			ArrayList<LuaValue> lvFighters = new ArrayList<LuaValue>();
			for(int team = 0; team < N_TEAMS; team++)
			{
				for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
				{
					Fighter f = fighters[team][id];
					if(f.health > 0)
					{
						lvFighters.add(f.toLuaValue());
					}
				}
			}
			
			return LuaValue.listOf(lvFighters.toArray(new LuaValue[lvFighters.size()]));
		}};
		
		public LuaValue getAllies = new ZeroArgFunction() { @Override public LuaValue call()
		{
			ArrayList<LuaValue> lvFighters = new ArrayList<LuaValue>();
			for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
			{
				Fighter f = fighters[fighter.team][id];
				if(f.health > 0)
				{
					lvFighters.add(f.toLuaValue());
				}
			}
			
			return LuaValue.listOf(lvFighters.toArray(new LuaValue[lvFighters.size()]));
		}};
		
		public LuaValue getEnemies = new ZeroArgFunction() { @Override public LuaValue call()
		{
			ArrayList<LuaValue> lvFighters = new ArrayList<LuaValue>();
			for(int team = 0; team < N_TEAMS; team++)
			{
				if(team != fighter.team)
				{
					for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
					{
						Fighter f = fighters[fighter.team][id];
						if(f.health > 0)
						{
							lvFighters.add(f.toLuaValue());
						}
					}
				}
			}
			
			return LuaValue.listOf(lvFighters.toArray(new LuaValue[lvFighters.size()]));
		}};
		
		public LuaValue findWeakestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter weakestEnemy = null;
			for(int team = 0; team < N_TEAMS; team++)
			{
				if(team != fighter.team)
				{
					for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
					{
						Fighter enemy = fighters[team][id];
						if(enemy.health > 0 && (weakestEnemy == null || enemy.health < weakestEnemy.health))
						{
							weakestEnemy = enemy;
						}
					}
				}
			}
			if(weakestEnemy != null) return weakestEnemy.toLuaValue();
			else return NIL;
		}};
	}

	@Override public void onServerStart() {}
	@Override public void onServerStop() {}
	@Override public void onClientDisconnected(Client client) {}
}
