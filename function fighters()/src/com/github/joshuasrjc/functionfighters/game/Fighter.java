package com.github.joshuasrjc.functionfighters.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.github.joshuasrjc.functionfighters.LuaFunctions;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.ui.Assets;


public class Fighter extends GameObject
{
	public LuaValue toLuaValue(int type)
	{
		LuaValue lv = super.toLuaValue();
		switch(type)
		{
		case ENEMY:
			setFighterTable(lv); 
			break;
			
		case ALLY:
			setFighterTable(lv);
			setAllyTable(lv);
			break;
			
		case THIS:
			setFighterTable(lv);
			setSelfTable(lv);
			break;
		}
		return lv;
	}
	
	private void setFighterTable(LuaValue lv)
	{
		lv.set("team", team + 1);
		lv.set("id", id + 1);
		lv.set("health", health);
		lv.set("maxHealth", MAX_HEALTH);
		lv.set("cooldown", cooldown);
		lv.set("shootCooldown", SHOOT_COOLDOWN);
		lv.set("bulletSpeed", BULLET_SPEED);
		lv.set("bulletDamage", BULLET_DAMAGE);
		lv.set("turnSpeed", TURN_SPEED);
		lv.set("isFacing", isFacing);
	}
	
	private void setAllyTable(LuaValue lv)
	{
		lv.set("sendMessage", sendMessage);
	}
	
	private void setSelfTable(LuaValue lv)
	{
		lv.set("moveForward", moveForward);
		lv.set("moveBackward", moveBackward);
		lv.set("moveLeft", moveLeft);
		lv.set("moveRight", moveRight);
		lv.set("moveToward", moveToward);
		lv.set("turnLeft", turnLeft);
		lv.set("turnRight", turnRight);
		lv.set("turnToward", turnToward);
		lv.set("shoot", shoot);
		lv.set("isFacingFighter", isFacingFighter);
		lv.set("isFacingAlly", isFacingAlly);
		lv.set("isFacingEnemy", isFacingEnemy);
	}
	
	public static final int ENEMY = 0;
	public static final int ALLY = 1;
	public static final int THIS = 2;
	
	public static final float FIGHTER_RADIUS = 12;
	public static final float MAX_HEALTH = 100;
	
	public static final float BULLET_SPEED = 8f;
	public static final float BULLET_DAMAGE = 20f;
	public static final int SHOOT_COOLDOWN = 50;
	public static final float TURN_SPEED = (float)Math.PI / 40;

	private static final float QUARTER_TURN = (float)Math.PI / 2f;

	public static final int UPDATE = 0;
	public static final int MESSAGE = 1;
	public static final int ON_HIT = 2;
	public static final int ON_FIGHTER_SHOT_BULLET = 3;
	public static final int ON_FIGHTER_DESTROYED = 4;
	
	private static final String[] EVENT_NAMES = 
	{
		"update",
		"recieveMessage",
		"onHit",
		"onFighterShotBullet",
		"onFighterDestroyed"
	};
	
	private LuaValue[] events;
	private Queue<GameEvent> inbox = new LinkedList<GameEvent>();
	
	public int team;
	public int id;
	
	public float health = 100;
	public int cooldown = 0;
	
	private BufferedImage sprite;
	
	private RayCastFilter fightersOnly = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj instanceof Fighter;
		}
	};
	
	private RayCastFilter alliesOnly = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj instanceof Fighter && ((Fighter)obj).team == team;
		}
	};
	
	private RayCastFilter enemiesOnly = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj instanceof Fighter && ((Fighter)obj).team != team;
		}
	};
	
	private Fighter self = this;
	private String script;
	private Server server;
	private Globals globals;

	Fighter(Game game, Vector2 position, float rotation, int team, int id, String script, Server server)
	{
		super(game, FIGHTER_RADIUS, position);

		this.rotation = rotation;
		this.team = team;
		this.id = id;
		this.script = script;
		this.server = server;
		
		thrust = .15f;
		maxSpeed = 4f;
	}
	
	public Fighter(ByteBuffer data)
	{
		super(data);
		this.health = data.getFloat();
		this.team = data.get();
		this.sprite = Assets.fighterSprites[team];
		this.uid = data.getShort();
	}
	
	@Override
	public void toByteBuffer(ByteBuffer data)
	{
		super.toByteBuffer(data);
		data.putFloat(health);
		data.put((byte)team);
		data.putShort((short)uid);
	}
	
	@Override
	public int getByteSize()
	{
		return GameObject.FIGHTER_BYTE_SIZE;
	}
	
	public String parseLuaError(LuaError error)
	{
		String str = "\nScript error in fighter #" + (id + 1) + " on team #" + (team + 1) + '\n';
		
		String message = error.getMessage();
		message = message.trim();
		
		String err = "";
		
		try
		{
			String infoLine = message.substring(message.lastIndexOf('\n'));
			infoLine = infoLine.trim();
			int i = 0;
			for(i = 0; !Character.isDigit(infoLine.charAt(i)); i++) {}
			infoLine = infoLine.substring(i);
			
			for(i = 0; Character.isDigit(infoLine.charAt(i)); i++) {}
			
			String lineString = infoLine.substring(0, i);
			
			String errorMessage = infoLine.substring(i + 1);
			errorMessage = errorMessage.trim();
			
			int lineNumber = Integer.parseInt(lineString);
			String line = script.split("\n")[lineNumber-1];
			
			err += "On line " + lineNumber + ":\n";
			err += line.trim() + '\n';
			err += errorMessage + '\n';
		}
		catch(Exception e) { }
		
		return str + err;
	}
	
	private void updateGlobals()
	{
		globals.set("game", game.toLuaValue(this));
		globals.set("this", this.toLuaValue(THIS));
	}
	
	public void initScript()
	{
		script = script.replace("!=", "~=");
		
		globals = JsePlatform.standardGlobals();
		globals.set("print", LuaFunctions.print(server));
		globals.set("Vector2", Vector2.toGlobalLuaValue());
		globals.set("os", LuaValue.NIL);
		LuaValue math = globals.get("math");
		math.set("random", LuaValue.NIL);
		math.set("randomseed", LuaValue.NIL);
		updateGlobals();
		try
		{
			LuaValue luaScript = globals.load(script);
			luaScript.call();
		}
		catch(LuaError error)
		{
			String message = parseLuaError(error);
			server.sendPacketToAllClients(new Packet(Packet.ERROR, message));
			game.stop();
		}
		
		events = new LuaValue[EVENT_NAMES.length];
		for(int i = 0; i < EVENT_NAMES.length; i++)
		{
			String name = EVENT_NAMES[i];
			events[i] = globals.get(name);
		}
	}
	
	public void addEvent(GameEvent ev)
	{
		inbox.add(ev);
	}
	
	private void sendEventToOtherFighters(int id)
	{
		for(Iterator<Fighter> it = game.getFighterIterator(); it.hasNext();)
		{
			Fighter f = it.next();
			LuaValue lv;
			if(f == this) continue;
			if(f.team == team)
			{
				lv = toLuaValue(ALLY);
			}
			else
			{
				lv = toLuaValue(ENEMY);
			}
			f.addEvent(new GameEvent(id, lv));
		}
	}
	
	public void call(int id, Varargs args)
	{
		LuaValue event = events[id];
		if(event.isfunction())
		{
			try
			{
				event.invoke(args);
			}
			catch(LuaError ex)
			{
				String message = parseLuaError(ex);
				server.sendPacketToAllClients(new Packet(Packet.ERROR, message));
				server.sendPacketToAllClients(new Packet(Packet.PLAY_SOUND, Assets.SOUND_ERROR_ID));
				destroy();
			}
		}
	}
	
	@Override
	public void update(int step)
	{
		if(step == Game.SCRIPT_STEP)
		{
			this.setAcceleration(Vector2.zero());
			this.setTurning(0);

			cooldown--;
			if(cooldown < 0) cooldown = 0;
			
			updateGlobals();
			
			while(!inbox.isEmpty())
			{
				GameEvent ev = inbox.poll();
				call(ev.type, ev.args);
			}
			
			call(UPDATE, LuaValue.NIL);

			
			if(this.getAcceleration().getMagnitude() > 1f)
			{
				this.setAcceleration(this.getAcceleration().normalized());
			}
			this.setAcceleration(this.getAcceleration().times(thrust));
			
			if(this.getTurning() > 1) this.setTurning(1f);
			if(this.getTurning() < -1) this.setTurning(-1f);
			this.setTurning(this.getTurning() * TURN_SPEED);
		}
		else
		{
			super.update(step);
		}
	}
	
	@Override
	public void draw(Graphics2D g)
	{
		int x = (int)(position.x);
		int y = (int)(position.y);
		
		int w = sprite.getWidth();
		int h = sprite.getHeight();
		
		AffineTransform trans = new AffineTransform();
		trans.translate(x, y);
		trans.rotate(rotation);
		trans.scale(2 * getRadius() / w, 2 * getRadius() / h);
		trans.translate(-sprite.getWidth()/2, -sprite.getHeight()/2);
		
		g.drawImage(sprite, trans, null);
		
		g.setColor(Color.GREEN);
		
		x -= (int)getRadius();
		y -= (int)getRadius();
		
		w = (int)(2*getRadius());
		h = (int)(getRadius()/3);
		
		y -= 2 * h;
		
		g.fillRect(x, y, (int)(w * health / 100f), h);
		
		g.setColor(Color.GRAY);
		g.setStroke(new BasicStroke(.01f));
		g.drawRect(x, y, w, h);
		
		g.setColor(Color.MAGENTA);
		//g.drawString("" + uid, position.x - getRadius(), position.y + 2 * getRadius());
	}
	
	public void shoot()
	{
		if(cooldown == 0)
		{
			cooldown = SHOOT_COOLDOWN;
			Vector2 bulletPos = this.getPosition();
			Vector2 look = new Vector2(getRotation());
			bulletPos.add(look.times(getRadius() + Bullet.BULLET_RADIUS));
			Bullet bullet = new Bullet(game, bulletPos, look.times(BULLET_SPEED), rotation, BULLET_DAMAGE, team, id);
			game.addObject(bullet);
			
			sendEventToOtherFighters(ON_FIGHTER_SHOT_BULLET);
			
			server.sendPacketToAllClients(new Packet(Packet.PLAY_SOUND, Assets.SOUND_SHOOT_ID));
		}
	}
	
	private void onDamageDealt(float damage)
	{
		server.sendPacketToAllClients(new Packet(Packet.PLAY_SOUND, Assets.SOUND_HIT_ID));
		if(health <= 0)
		{
			sendEventToOtherFighters(ON_FIGHTER_DESTROYED);
			server.sendPacketToAllClients(new Packet(Packet.PLAY_SOUND, Assets.SOUND_EXPLOSION_ID));
			destroy();
		}
		else
		{
			addEvent(new GameEvent(ON_HIT, LuaValue.valueOf(damage)));
		}
	}
	
	public void dealDamage(float damage)
	{
		health -= damage;
		onDamageDealt(damage);
	}

	public void moveInDirection(float theta, float speed)
	{
		addForce(new Vector2(speed * (float)Math.cos(theta), speed * (float)Math.sin(theta)));
	}
	
	private LuaValue moveForward = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation(), speed.tofloat());
		else self.moveInDirection(self.getRotation(), 1f);
		return NIL;
	}};
	
	private LuaValue moveBackward = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + 2 * QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + 2 * QUARTER_TURN, 1f);
		return NIL;
	}};
	
	private LuaValue moveLeft = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + 3 * QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + 3 * QUARTER_TURN, 1f);
		return NIL;
	}};
	
	private LuaValue moveRight = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + QUARTER_TURN, 1f);
		return NIL;
	}};
	
	private LuaValue moveToward = new ThreeArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1, LuaValue arg2)
	{
		float speed = 1;
		if(arg2.isnumber()) speed = arg2.tofloat();
		else if(arg0.istable() && arg1.isnumber()) speed = arg1.tofloat();
		
		Vector2 p = new Vector2(arg0, arg1);
		p.subtract(self.position);
		p.normalize();
		p.times(speed);
		
		self.addForce(p);
		return NIL;
	}};
	
	private LuaValue turnLeft = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.addTorque(-speed.tofloat());
		else self.addTorque(-1f);
		return NIL;
	}};
	
	private LuaValue turnRight = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.addTorque(speed.tofloat());
		else self.addTorque(1f);
		return NIL;
	}};
	
	private LuaValue turnToward = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		Vector2 p = new Vector2(arg0, arg1);
		p.subtract(getPosition());
		float theta = (float)Math.atan2(p.y, p.x);
		
		theta -= getRotation();
		while(theta < -PI) theta += TWOPI;
		while(theta > PI) theta -= TWOPI;
		
		float speed = theta / TURN_SPEED;
		addTorque(speed);
		return NIL;
	}};
	
	private LuaValue shoot = new ZeroArgFunction() { @Override public LuaValue call()
	{
		self.shoot();
		return NIL;
	}};

	private LuaValue isFacingFighter = new ZeroArgFunction() { @Override public LuaValue call()
	{
		return LuaValue.valueOf(game.castRay(position, new Vector2(rotation), 0, false, fightersOnly).didHitObject());
	}};

	private LuaValue isFacingAlly = new ZeroArgFunction() { @Override public LuaValue call()
	{
		return LuaValue.valueOf(game.castRay(position, new Vector2(rotation), 0, false, alliesOnly).didHitObject());
	}};

	private LuaValue isFacingEnemy = new ZeroArgFunction() { @Override public LuaValue call()
	{
		return LuaValue.valueOf(game.castRay(position, new Vector2(rotation), 0, false, enemiesOnly).didHitObject());
	}};
	
	private LuaValue sendMessage = new VarArgFunction() { @Override public LuaValue invoke(Varargs args)
	{
		inbox.add(new GameEvent(MESSAGE, args));
		return NIL;
	}};
	
	private LuaValue isFacing = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		arg0.checktable();
		LuaValue lvuid = arg0.get("uid");
		long uid = lvuid.checkint();
		
		float tolerance = 0;
		if(arg1.isnumber())
		{
			tolerance = arg1.tofloat();
		}
		
		RayCastFilter filter = new RayCastFilter()
		{
			@Override
			public boolean doTest(GameObject obj)
			{
				return obj.uid == uid;
			}
	
		};
		
		RayCastResult result = game.castRay(position, new Vector2(rotation), tolerance, false, filter);
		return LuaValue.valueOf(result.didHitObject());
	}};
}
