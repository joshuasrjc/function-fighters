package com.github.joshuasrjc.functionfighters.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.luaj.vm2.LuaValue;

public class GameObject
{
	public LuaValue toLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		lv.set("radius", radius);
		lv.set("position", position.toLuaValue());
		lv.set("velocity", velocity.toLuaValue());
		lv.set("rotation", rotation);
		lv.set("mass", mass);
		lv.set("elasticity", elasticity);
		lv.set("friction", friction);
		return lv;
	}
	
	public static final byte NULL_ID = 0;
	public static final byte OBJECT_ID = 1;
	public static final byte FIGHTER_ID = 2;
	public static final byte BULLET_ID = 3;
	public static final int OBJECT_BYTE_SIZE = 16;
	public static final int FIGHTER_BYTE_SIZE = 21;
	public static final int BULLET_BYTE_SIZE = 16;
	
	public static final float PI = (float)Math.PI;
	public static final float TWOPI = (float)Math.PI * 2f;
	
	protected Game game;
	private GameObject self = this;
	
	private float radius;
	
	protected Vector2 position;
	protected Vector2 velocity;
	protected Vector2 acceleration;
	
	protected float maxSpeed = -1;
	
	protected float rotation;
	protected float turning;
	
	protected float maxTurning = -1;
	
	protected float mass;
	protected float elasticity = 1.0f;
	protected float friction = 0.0f;
	
	protected boolean canCollide = true;
	protected boolean destroyed = false;
	
	private Vector2 collisionPoint = null;
	private Vector2 unstickVector = new Vector2(0, 0);
	private RayCastFilter collisionFilter = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj.canCollide && obj != self;
		}
	};
	
	GameObject(Game game)
	{
		this.game = game;
		setRadius(0);
		this.position = Vector2.zero();
		this.velocity = Vector2.zero();
		this.acceleration = Vector2.zero();
		this.rotation = 0;
		this.turning = 0;
		this.turning = 0;
	}
	
	GameObject(Game game, float radius, Vector2 position)
	{
		this.game = game;
		setRadius(radius);
		this.position = new Vector2(position);
		this.velocity = Vector2.zero();
		this.acceleration = Vector2.zero();
		this.rotation = 0;
		this.turning = 0;
		this.turning = 0;
	}
	
	public GameObject(ByteBuffer data)
	{
		this.setRadius(data.getFloat());
		this.position = Vector2.zero();
		this.position.x = data.getFloat();
		this.position.y = data.getFloat();
		this.rotation = data.getFloat();
	}
	
	public void toByteBuffer(ByteBuffer data)
	{
		data.putFloat(radius);
		data.putFloat(position.x);
		data.putFloat(position.y);
		data.putFloat(rotation);
	}
	
	public int getByteSize()
	{
		return OBJECT_BYTE_SIZE;
	}
	
	public void setRadius(float radius)
	{
		this.radius = radius;
		this.mass = (float)Math.PI * radius * radius;
	}

	public void setPosition(Vector2 v) { this.position = new Vector2(v); }
	public void setVelocity(Vector2 v) { this.velocity = new Vector2(v); }
	public void setAcceleration(Vector2 v) { this.acceleration = new Vector2(v); }
	public void setMaxSpeed(float f) { this.maxSpeed = f; }
	
	public void setRotation(float f)
	{
		while(f < 0) f += TWOPI;
		while(f > TWOPI) f -= TWOPI;
		this.rotation = f;
	}
	
	public void setTurning(float f) { this.turning = f; }
	public void setMaxTurning(float f) { this.maxTurning = f; }
	public void setElasticity(float f) { this.elasticity = f; }
	public void setFriction(float f) { this.friction = f; }
	public void setCanCollide(boolean b) { this.canCollide = b; }
	
	public float getRadius() { return radius; }
	public Vector2 getPosition() { return new Vector2(position); }
	public Vector2 getVelocity() { return new Vector2(velocity); }
	public Vector2 getAcceleration() { return new Vector2(acceleration); }
	public float getMaxSpeed(float f) { return maxSpeed; }
	public float getRotation() { return rotation; }
	public float getTurning() { return turning; }
	public float getMaxTurning() { return maxTurning; }
	public float getElasticity() { return elasticity; }
	public float getFriction() { return friction; }
	public boolean getCanCollide() { return canCollide; }
	public boolean isDestroyed() { return destroyed; }
	
	public void addForce(Vector2 force)
	{
		acceleration.add(force);
	}
	
	public void addTorque(float torque)
	{
		turning += torque;
	}
	
	public void destroy()
	{
		destroyed = true;
	}
	
	public float getMass()
	{
		return mass;
	}
	
	private void clampVelocity()
	{
		if(maxSpeed >= 0 && velocity.getMagnitude() > maxSpeed)
		{
			velocity.divide(velocity.getMagnitude());
			velocity.multiply(maxSpeed);
		}
	}
	
	private void collideWithWalls()
	{
		if(position.y < Game.TOP + radius)
		{
			velocity = Vector2.zero();
			position.y = Game.TOP + radius;
			onCollide(null);
		}
		if(position.y > Game.BOTTOM - radius)
		{
			velocity = Vector2.zero();
			position.y = Game.BOTTOM - radius;
			onCollide(null);
		}
		if(position.x < Game.LEFT + radius)
		{
			velocity = Vector2.zero();
			position.x = Game.LEFT + radius;
			onCollide(null);
		}
		if(position.x > Game.RIGHT - radius)
		{
			velocity = Vector2.zero();
			position.x = Game.RIGHT - radius;
			onCollide(null);
		}
	}
	
	public void update(int step)
	{
		if(step == Game.PHYSICS_CALC_STEP)
		{
			velocity.add(acceleration);
			clampVelocity();
		}
		if(step == Game.COLLISION_CALC_STEP)
		{
			collisionPoint = null;
			RayCastResult result = game.castRay(position, velocity, radius, true, collisionFilter);
			if(result.didHitObject() && !result.inside)
			{
				velocity = Vector2.zero();
				collisionPoint = result.hitPoint;
				onCollide(result.hitObject);
			}
		}
		else if(step == Game.COLLISION_MOVE_STEP)
		{
			if(canCollide && collisionPoint != null)
			{
				position.add(collisionPoint);
				position.divide(2f);
			}
		}
		else if(step == Game.UNSTICK_CALC_STEP)
		{
			if(canCollide)
			{
				unstickVector = new Vector2(0, 0);
				for(Iterator<GameObject> it = game.getObjectIterator(); it.hasNext();)
				{
					GameObject obj = it.next();
					if(!obj.canCollide || obj == this) continue;
					
					Vector2 v = obj.position.minus(position);
					float dist = v.getMagnitude();
					if(dist < (radius + obj.radius)*1.01 && dist != 0)
					{
						v.divide(dist);
						dist = radius + obj.radius - dist;
						v.multiply(-dist);
						unstickVector.add(v);
					}
				}
			}
		}
		else if(step == Game.UNSTICK_MOVE_STEP)
		{
			if(canCollide)
			{
				position.add(unstickVector);
			}
		}
		else if(step == Game.PHYSICS_MOVE_STEP)
		{
			position.add(velocity);
			
			velocity.multiply(1.0f - friction);
			setRotation(rotation + turning);
			collideWithWalls();
		}
	}
	
	public void onCollide(GameObject obj)
	{
		
	}
	
	public void draw(Graphics2D g)
	{
		g.setColor(Color.LIGHT_GRAY);
		g.fillOval((int)(position.x - radius), (int)(position.y - radius), (int)(2 * radius), (int)(2 * radius));
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(3));
		g.drawOval((int)(position.x - radius), (int)(position.y - radius), (int)(2 * radius), (int)(2 * radius));
	}
}
