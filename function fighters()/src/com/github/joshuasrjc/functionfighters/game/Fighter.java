package com.github.joshuasrjc.functionfighters.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.github.joshuasrjc.functionfighters.LuaFunctions;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.ui.Sprites;


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
			
		case SELF:
			setFighterTable(lv);
			setSelfTable(lv);
			break;
		}
		return lv;
	}
	
	private void setFighterTable(LuaValue lv)
	{
		lv.set("team", team);
		lv.set("id", id);
		lv.set("health", health);
		lv.set("maxHealth", MAX_HEALTH);
		lv.set("cooldown", cooldown);
		lv.set("shootCooldown", SHOOT_COOLDOWN);
		lv.set("bulletSpeed", BULLET_SPEED);
		lv.set("bulletDamage", BULLET_DAMAGE);
		lv.set("thrust", THRUST);
		lv.set("turnSpeed", TURN_SPEED);
	}
	
	private void setAllyTable(LuaValue lv)
	{
		
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
	}
	
	public static final int ENEMY = 0;
	public static final int ALLY = 1;
	public static final int SELF = 2;
	
	public static final float FIGHTER_RADIUS = 8;
	public static final float MAX_HEALTH = 100;
	
	public static final float BULLET_SPEED = 50f;
	public static final float BULLET_DAMAGE = 25f;
	public static final int SHOOT_COOLDOWN = 50;
	public static final float THRUST = 1f;
	public static final float TURN_SPEED = (float)Math.PI / 20;

	private static final float QUARTER_TURN = (float)Math.PI / 2f;

	public int team;
	public int id;
	
	public float health = 100;
	public int cooldown = 0;
	
	private BufferedImage sprite = Sprites.fighterSprite;
	
	private Fighter self = this;
	private String script;
	private Server server;
	private Globals globals;
	private LuaValue update;

	Fighter(Game game, Vector2 position, int team, int id, String script, Server server)
	{
		super(game, FIGHTER_RADIUS, position);

		this.team = team;
		this.id = id;
		this.script = script;
		this.server = server;

		this.setSpriteIndex((byte)1);
		this.setElasticity(0.9f);
		this.setFriction(0.1f);
		this.setMaxSpeed(8f);
		
		globals = JsePlatform.standardGlobals();
		globals.set("print", LuaFunctions.print(server));
		LuaValue luaScript = globals.load(script);
		luaScript.call();
		update = globals.get("update");
	}
	
	public Fighter(ByteBuffer data)
	{
		super(data);
		this.health = data.getFloat();
	}
	
	@Override
	public void toByteBuffer(ByteBuffer data)
	{
		super.toByteBuffer(data);
		data.putFloat(health);
	}
	
	@Override
	public int getByteSize()
	{
		return GameObject.FIGHTER_BYTE_SIZE;
	}
	
	public String parseLuaError(LuaError error)
	{
		String message = "\nScript error in fighter #" + this.id + " on team #" + this.team + '\n';
		
		String line = error.getMessage();
		line = line.substring(line.lastIndexOf(':') + 1);
		String err = line.substring(line.indexOf(' ') + 1);
		line = line.substring(0, line.indexOf(' '));
		
		message += "On line: " + line + '\n';
		
		try
		{
			int lineNumber = Integer.parseInt(line);
			System.out.println(lineNumber);
			line = error.getMessage().split("\n")[lineNumber - 1];
			line = line.trim();
			message += line + '\n';
		}
		catch(Exception ex)
		{
			
		}
		
		message += err + '\n';
		
		return message;
	}
	
	@Override
	public void preUpdate()
	{
		this.setAcceleration(Vector2.zero());
		this.setTurning(0);
		
		if(update.isfunction())
		{
			try
			{
				update.call(game.toLuaValue(this), this.toLuaValue(SELF));
			}
			catch(LuaError ex)
			{
				String message = parseLuaError(ex);
				server.sendPacketToAllClients(new Packet(Packet.ERROR, message));
				destroy();
			}
		}
		
		if(this.getAcceleration().getMagnitude() > 1f)
		{
			this.setAcceleration(this.getAcceleration().normalized());
		}
		this.setAcceleration(this.getAcceleration().times(THRUST));
		
		if(this.getTurning() > 1) this.setTurning(1f);
		if(this.getTurning() < -1) this.setTurning(-1f);
		this.setTurning(this.getTurning() * TURN_SPEED);
	}
	
	@Override
	public void update()
	{
		super.update();
		
		cooldown--;
		if(cooldown < 0) cooldown = 0;
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
		h = (int)(getRadius()/5);
		
		y -= 2 * h;
		
		w = (int)(w * (health / 100f));
		
		g.fillRect(x, y, w, h);
	}
	
	public void shoot()
	{
		if(cooldown == 0)
		{
			cooldown = SHOOT_COOLDOWN;
			Vector2 bulletPos = this.getPosition();
			Vector2 look = new Vector2(getRotation());
			bulletPos.add(look.times(getRadius() + Bullet.BULLET_RADIUS));
			Bullet bullet = new Bullet(game, bulletPos, look.times(BULLET_SPEED), BULLET_DAMAGE, team, id);
			game.objects.add(bullet);
		}
	}
	
	public void onDamageDealt(float damage)
	{
		if(health <= 0)
		{
			destroy();
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
}
