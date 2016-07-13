package com.github.joshuasrjc.functionfighters.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;

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
		
		lv.set("getDistanceTo", gf.getDistanceTo);
		lv.set("getDirectionTo", gf.getDirectionTo);
		
		lv.set("getFighters", gf.getFighters); 
		lv.set("getAllies", gf.getAllies);
		lv.set("getEnemies", gf.getEnemies);
		
		lv.set("findClosestFighter", gf.findClosestFighter);
		lv.set("findClosestAlly", gf.findClosestAlly);
		lv.set("findClosestEnemy", gf.findClosestEnemy);
		
		lv.set("findFarthestFighter", gf.findFarthestFighter);
		lv.set("findFarthestAlly", gf.findFarthestAlly);
		lv.set("findFarthestEnemy", gf.findFarthestEnemy);
		
		lv.set("findWeakestFighter", gf.findWeakestFighter);
		lv.set("findWeakestAlly", gf.findWeakestAlly);
		lv.set("findWeakestEnemy", gf.findWeakestEnemy);
		
		lv.set("findStrongestFighter", gf.findStrongestFighter);
		lv.set("findStrongestAlly", gf.findStrongestAlly);
		lv.set("findStrongestEnemy", gf.findStrongestEnemy);
		
		return lv;
	}
	
	public static final int N_TEAMS = 2;
	public static final int N_FIGHTERS_PER_TEAM = 3;
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
	public ArrayList<Fighter> fighters = new ArrayList<Fighter>();
	
	public Vector2[][] positions = {
		{new Vector2(-400, 0), new Vector2(-400, -200), new Vector2(-400, 200)},
		{new Vector2(400, 0), new Vector2(400, 200), new Vector2(400, -200)}
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
				fighters.add(fighter);
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
	
	private float getRayCastDistance(Vector2 start, Vector2 dir, float radius, GameObject obj)
	{
		float a = start.x;
		float b = start.y;
		float c = dir.x;
		float d = dir.y;
		float h = obj.position.x;
		float k = obj.position.y;
		float r = obj.getRadius() + radius;
		
		float a2 = a*a;
		float b2 = b*b;
		float c2 = c*c;
		float d2 = d*d;
		float h2 = h*h;
		float k2 = k*k;
		float r2 = r*r;
		
		float radicand = (d2 + c2)*r2 - c2*k2 + (2*c*d*h - 2*a*c*d + 2*b*c2)*k - d2*h2 + (2*a*d2 - 2*b*c*d)*h - a2*d2 + 2*a*b*c*d - b2*c2;
		if(radicand < 0f) return Float.NaN;
		
		float root = (float)Math.sqrt(radicand);
		float addend = d*k + c*h - b*d - a*c;
		float divisor = d2 + c2;
		
		float t1 = (-root + addend) / divisor;
		float t2 = (root + addend) / divisor;
		
		float dist1 = Float.NaN;
		float dist2 = Float.NaN;
		
		if(t1 >= 0 && t1 <= 1)
		{
			Vector2 p1 = new Vector2(a + c*t1, b + d*t1);
			dist1 = p1.minus(start).getMagnitude();
		}
		
		if(t2 >= 0 && t2 <= 1)
		{
			Vector2 p2 = new Vector2(a + c*t2, b + d*t2);
			dist2 = p2.minus(start).getMagnitude();
		}
		
		if(Float.isNaN(dist1) && Float.isNaN(dist2))
		{
			return Float.NaN;
		}
		else if(Float.isNaN(dist1))
		{
			return dist2;
		}
		else if(Float.isNaN(dist2))
		{
			return dist1;
		}
		else if(dist1 <= dist2)
		{
			return dist1;
		}
		else if(dist2 < dist1)
		{
			return dist2;
		}
		
		return Float.NaN;
	}
	
	public GameObject castRay(Vector2 start, Vector2 dir, float radius, RayCastFilter filter)
	{
		GameObject closestObject = null;
		float closestDistance = Float.MAX_VALUE;
		for(int i = 0; i < objects.size(); i++)
		{
			GameObject obj = objects.get(i);
			if(filter == null || filter.doTest(obj))
			{
				float distance = getRayCastDistance(start, dir, radius, obj);
				if(!Float.isNaN(distance) && distance < closestDistance)
				{
					closestDistance = distance;
					closestObject = obj;
				}
			}
		}
		return closestObject;
	}
	
	public float getDistanceBetween(GameObject obj1, GameObject obj2)
	{
		return obj1.position.minus(obj2.position).getMagnitude();
	}

	private class GameFunctions
	{
		Fighter fighter;
		GameFunctions functions = this;
		
		private static final int FIGHTERS = 0;
		private static final int ALLIES = 1;
		private static final int ENEMIES = 2;
		
		GameFunctions(Fighter fighter)
		{
			this.fighter = fighter;
		}
		
		private Fighter[] get(int team)
		{
			ArrayList<Fighter> fighterList = new ArrayList<Fighter>();
			for(int i = 0; i < fighters.size(); i++)
			{
				Fighter f = fighters.get(i);
				if(f != fighter && ( team == FIGHTERS || (team == ALLIES) == (f.team == fighter.team) ))
				{
					fighterList.add(f);
				}
			}
			return fighterList.toArray(new Fighter[fighterList.size()]);
		}
		
		private Fighter findClosest(int team)
		{
			Fighter closestFighter = null;
			float shortestDistance = Float.MAX_VALUE;
			for(int i = 0; i < fighters.size(); i++)
			{
				Fighter f = fighters.get(i);
				if(f != fighter && ( team == FIGHTERS || (team == ALLIES) == (f.team == fighter.team) ))
				{
					float distance = getDistanceBetween(f, fighter);
					if(distance < shortestDistance)
					{
						shortestDistance = distance;
						closestFighter = f;
					}
				}
			}
			return closestFighter;
		}
		
		private Fighter findFarthest(int team)
		{
			Fighter farthestFighter = null;
			float longestDistance = Float.MIN_VALUE;
			for(int i = 0; i < fighters.size(); i++)
			{
				Fighter f = fighters.get(i);
				if(f != fighter && ( team == FIGHTERS || (team == ALLIES) == (f.team == fighter.team) ))
				{
					float distance = getDistanceBetween(f, fighter);
					if(distance > longestDistance)
					{
						longestDistance = distance;
						farthestFighter = f;
					}
				}
			}
			return farthestFighter;
		}
		
		private Fighter findWeakest(int team)
		{
			Fighter weakestFighter = null;
			float lowestHealth = Float.MAX_VALUE;
			for(int i = 0; i < fighters.size(); i++)
			{
				Fighter f = fighters.get(i);
				if(f != fighter && ( team == FIGHTERS || (team == ALLIES) == (f.team == fighter.team) ))
				{
					float health = f.health;
					if(health < lowestHealth)
					{
						lowestHealth = health;
						weakestFighter = f;
					}
				}
			}
			return weakestFighter;
		}
		
		private Fighter findStrongest(int team)
		{
			Fighter strongestFighter = null;
			float highestHealth = Float.MIN_VALUE;
			for(int i = 0; i < fighters.size(); i++)
			{
				Fighter f = fighters.get(i);
				if(f != fighter && ( team == FIGHTERS || (team == ALLIES) == (f.team == fighter.team) ))
				{
					float health = f.health;
					if(health > highestHealth)
					{
						highestHealth = health;
						strongestFighter = f;
					}
				}
			}
			return strongestFighter;
		}
		
		
		
		
		public LuaValue getDistanceTo = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
		{
			Vector2 p = new Vector2(arg0, arg1);
			p.subtract(fighter.position);
			return LuaValue.valueOf(p.getMagnitude());
		}};
		
		public LuaValue getDirectionTo = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
		{
			Vector2 p = new Vector2(arg0, arg1);
			p.subtract(fighter.position);
			return LuaValue.valueOf(Math.atan2(p.y, p.x));
		}};
		
		
		
		
		public LuaValue getFighters = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter[] fighters = functions.get(FIGHTERS);
			LuaValue[] lvs = new LuaValue[fighters.length];
			for(int i = 0; i < lvs.length; i++)
			{
				if(fighters[i].team == fighter.team)
				{
					lvs[i] = fighters[i].toLuaValue(Fighter.ALLY);
				}
				else
				{
					lvs[i] = fighters[i].toLuaValue(Fighter.ENEMY);
				}
			}
			return LuaValue.listOf(lvs);
		}};
		
		public LuaValue getAllies = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter[] fighters = functions.get(ALLIES);
			LuaValue[] lvs = new LuaValue[fighters.length];
			for(int i = 0; i < lvs.length; i++)
			{
				lvs[i] = fighters[i].toLuaValue(Fighter.ALLY);
			}
			return LuaValue.listOf(lvs);
		}};
		
		public LuaValue getEnemies = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter[] fighters = functions.get(ENEMIES);
			LuaValue[] lvs = new LuaValue[fighters.length];
			for(int i = 0; i < lvs.length; i++)
			{
				lvs[i] = fighters[i].toLuaValue(Fighter.ENEMY);
			}
			return LuaValue.listOf(lvs);
		}};
		
		
		
		
		
		public LuaValue findClosestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findClosest(FIGHTERS);
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findClosestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findClosest(ALLIES);
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findClosestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findClosest(ENEMIES);
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findFarthestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(FIGHTERS);
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findFarthestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(ALLIES);
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findFarthestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(ENEMIES);
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findWeakestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(FIGHTERS);
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findWeakestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(ALLIES);
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findWeakestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(ENEMIES);
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findStrongestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(FIGHTERS);
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findStrongestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(ALLIES);
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findStrongestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(ENEMIES);
			return f.toLuaValue(Fighter.ENEMY);
		}};
	}

	@Override public void onServerStart() {}
	@Override public void onServerStop() {}
	@Override public void onClientDisconnected(Client client) {}
}
