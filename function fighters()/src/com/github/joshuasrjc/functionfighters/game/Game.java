package com.github.joshuasrjc.functionfighters.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

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
	
	public static final int DEBUG_STEP = 0;
	public static final int SCRIPT_STEP = 1;
	public static final int PHYSICS_CALC_STEP = 2;
	public static final int COLLISION_CALC_STEP = 3;
	public static final int COLLISION_MOVE_STEP = 4;
	public static final int UNSTICK_CALC_STEP = 5;
	public static final int UNSTICK_MOVE_STEP = 6;
	public static final int PHYSICS_MOVE_STEP = 7;
	
	public static final int N_TEAMS = 2;
	public static final int N_FIGHTERS_PER_TEAM = 4;
	public static final long FRAME_MILLIS = 20;
	
	public static final float WIDTH = 800;
	public static final float HEIGHT = 400;
	public static final float TOP = -HEIGHT/2;
	public static final float BOTTOM = HEIGHT/2;
	public static final float LEFT = -WIDTH/2;
	public static final float RIGHT = WIDTH/2;
	
	private Game game = this;
	private Server server;
	
	private ArrayList<String> scriptNames = new ArrayList<String>();
	private ArrayList<String> scripts = new ArrayList<String>();
	private int[] selections = new int[N_TEAMS];
	
	private boolean gameRunning = false;
	private boolean gamePaused = false;
	private long time = 0;
	private Frame lastFrame = null;
	
	public static final float G = 0.0f;
	
	private Thread thread;
	
	private ArrayList<GameObject> objects = new ArrayList<GameObject>();
	private ArrayList<Fighter> fighters = new ArrayList<Fighter>();
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();
	
	private Queue<GameObject> toBeAdded = new LinkedList<GameObject>();
	
	public Vector2[][] positions = {
		{new Vector2(-400, 0), new Vector2(-400, -200), new Vector2(-400, 200)},
		{new Vector2(400, 0), new Vector2(400, 200), new Vector2(400, -200)}
	};
	
	public float[] rotations = {
			0f,
			Fighter.PI
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
	
	public void addObject(GameObject obj)
	{
		toBeAdded.add(obj);
	}
	
	public Iterator<GameObject> getObjectIterator()
	{
		return objects.iterator();
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
		if(isRunning())
		{
			client.sendPacket(new Packet(Packet.GAME_START));
			if(isPaused())
			{
				client.sendPacket(new Packet(Packet.GAME_PAUSE));
				if(lastFrame != null) client.sendPacket(lastFrame.toPacket());
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
				if(game.start())
				{
					server.sendPacketToAllClients(new Packet(Packet.GAME_START));
					server.sendPacketToAllClients(new Packet(Packet.INFO, "Game Started"));
				}
			}
			else if(game.isPaused())
			{
				game.resume();
				server.sendPacketToAllClients(new Packet(Packet.GAME_START));
				server.sendPacketToAllClients(new Packet(Packet.INFO, "Game Resumed"));
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
					server.sendPacketToAllClients(new Packet(Packet.INFO, "Game Paused"));
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
				server.sendPacketToAllClients(new Packet(Packet.GAME_STOP));
				server.sendPacketToAllClients(new Packet(Packet.INFO, "Game Stopped"));
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
	
	public void buildPositions(float radius)
	{
		positions = new Vector2[N_TEAMS][N_FIGHTERS_PER_TEAM];
		float alpha = (float)Math.asin(BOTTOM/radius);
		float beta = 2f * alpha / (N_FIGHTERS_PER_TEAM + 1);
		for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
		{
			float theta = -alpha + (id + 1) * beta;
			positions[0][id] = new Vector2(theta + (float)Math.PI).times(radius);
			positions[1][id] = new Vector2(theta).times(radius);
		}
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
		
		buildPositions(500);
		
		for(int team = 0; team < N_TEAMS; team++)
		{
			for(int id = 0; id < N_FIGHTERS_PER_TEAM; id++)
			{
				Fighter fighter = new Fighter(this, positions[team][id], rotations[team], team, id, scripts[team], server);
				addObject(fighter);
			}
		}
		
		addObjects();
		
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
		
		try
		{
			thread.join();
		}
		catch (Exception ex)
		{
			ChatLog.logError("Error stopping game.");
		}
		
		while(objects.size() > 0) objects.remove(objects.get(0));
		while(fighters.size() > 0) fighters.remove(fighters.get(0));
		while(bullets.size() > 0) bullets.remove(bullets.get(0));
	}
	
	public void update(int step)
	{
		for(GameObject obj : objects)
		{
			obj.update(step);
		}
		addObjects();
	}
	
	private void addObjects()
	{
		while(!toBeAdded.isEmpty())
		{
			GameObject obj = toBeAdded.poll();
			objects.add(obj);
			if(obj instanceof Fighter) fighters.add((Fighter)obj);
			if(obj instanceof Bullet) bullets.add((Bullet)obj);
		}
	}
	
	private void cleanup()
	{
		for(Iterator<GameObject> it = objects.iterator(); it.hasNext();)
		{
			GameObject obj = it.next();
			if(obj.isDestroyed())
			{
				it.remove();
			}
		}
		for(Iterator<Fighter> it = fighters.iterator(); it.hasNext();)
		{
			GameObject obj = it.next();
			if(obj.isDestroyed())
			{
				it.remove();
			}
		}
		for(Iterator<Bullet> it = bullets.iterator(); it.hasNext();)
		{
			GameObject obj = it.next();
			if(obj.isDestroyed())
			{
				it.remove();
			}
		}
	}
	
	public void sendFrame()
	{
		GameObject[] objectArray = new GameObject[objects.size()];
		Iterator<GameObject> it = objects.iterator();
		for(int i = 0; it.hasNext(); i++)
		{
			objectArray[i] = it.next();
		}
		Frame frame = new Frame(objectArray);
		lastFrame = frame;
		server.sendPacketToAllClients(frame.toPacket());
	}
	
	@Override
	public void run()
	{
		while(gameRunning)
		{
			update(DEBUG_STEP);
			update(SCRIPT_STEP);
			update(PHYSICS_CALC_STEP);
			update(COLLISION_CALC_STEP);
			update(COLLISION_MOVE_STEP);
			update(UNSTICK_CALC_STEP);
			update(UNSTICK_MOVE_STEP);
			update(PHYSICS_MOVE_STEP);
			cleanup();
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
	
	public RayCastResult castRay(Vector2 start, Vector2 dir, float radius, boolean bounded, RayCastFilter filter)
	{
		GameObject hitObject = null;
		double smallestT = Double.MAX_VALUE;
		Vector2 hitPoint = null;
		boolean inside = false;
		
		double a = start.x;
		double b = start.y;
		double c = dir.x;
		double d = dir.y;
		
		if(c == 0 && d == 0) return new RayCastResult(null, null, false);
		
		double a2 = a*a;
		double b2 = b*b;
		double c2 = c*c;
		double d2 = d*d;
		
		objectLoop: for(GameObject obj : objects)
		{
			if(filter == null || filter.doTest(obj))
			{
				double h = obj.position.x;
				double k = obj.position.y;
				double r = obj.getRadius() + radius;
				
				double h2 = h*h;
				double k2 = k*k;
				double r2 = r*r;
				
				double radicand = (d2 + c2)*r2 - c2*k2 + (2*c*d*h - 2*a*c*d + 2*b*c2)*k - d2*h2 + (2*a*d2 - 2*b*c*d)*h - a2*d2 + 2*a*b*c*d - b2*c2;
				if(radicand < 0)
				{
					continue objectLoop;
				}
				
				double root = Math.sqrt(radicand);
				double addend = d*k + c*h - b*d - a*c;
				double divisor = d2 + c2;
				
				double t1 = (addend - root) / divisor;
				double t2 = (addend + root) / divisor;
				
				if(t1 >= 0)
				{
					if((t1 <= 1 || !bounded) && t1 < smallestT)
					{
						smallestT = t1;
						hitObject = obj;
						hitPoint = new Vector2((float)(a + c*t1), (float)( b + d*t1));
						inside = false;
					}
				}
				else if(t2 >= 0 && t1 < smallestT)
				{
					smallestT = t1;
					hitObject = obj;
					hitPoint = new Vector2((float)(a + c*t1), (float)( b + d*t1));
					inside = true;
				}
			}
		}
		
		return new RayCastResult(hitObject, hitPoint, inside);
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
			for(Fighter f : fighters)
			{
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
			for(Fighter f : fighters)
			{
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
			for(Fighter f : fighters)
			{
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
			for(Fighter f : fighters)
			{
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
			for(Fighter f : fighters)
			{
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
		
		public LuaValue getBullets = new ZeroArgFunction() { @Override public LuaValue call()
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
			if(f == null) return NIL;
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findClosestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findClosest(ALLIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findClosestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findClosest(ENEMIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findFarthestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(FIGHTERS);
			if(f == null) return NIL;
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findFarthestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(ALLIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findFarthestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findFarthest(ENEMIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findWeakestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(FIGHTERS);
			if(f == null) return NIL;
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findWeakestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(ALLIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findWeakestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findWeakest(ENEMIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ENEMY);
		}};
		
		
		
		
		
		public LuaValue findStrongestFighter = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(FIGHTERS);
			if(f == null) return NIL;
			LuaValue lv;
			if(f.team == fighter.team) lv = f.toLuaValue(Fighter.ALLY);
			else lv = f.toLuaValue(Fighter.ENEMY);
			return lv;
		}};
		
		public LuaValue findStrongestAlly = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(ALLIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ALLY);
		}};
		
		public LuaValue findStrongestEnemy = new ZeroArgFunction() { @Override public LuaValue call()
		{
			Fighter f = findStrongest(ENEMIES);
			if(f == null) return NIL;
			return f.toLuaValue(Fighter.ENEMY);
		}};
	}

	@Override public void onServerStart() {}
	@Override public void onServerStop() {}
	@Override public void onClientDisconnected(Client client) {}
}
