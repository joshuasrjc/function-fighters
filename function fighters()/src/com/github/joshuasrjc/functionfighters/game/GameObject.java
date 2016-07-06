package com.github.joshuasrjc.functionfighters.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.luaj.vm2.LuaValue;

import com.github.joshuasrjc.functionfighters.ui.Sprites;

public class GameObject
{
	public LuaValue toLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		lv.set("radius", radius);
		lv.set("position", position.toLuaValue());
		lv.set("velocity", velocity.toLuaValue());
		lv.set("rotation", rotation);
		lv.set("rotationalVelocity", rotationalVelocity);
		lv.set("mass", mass);
		lv.set("elasticity", elasticity);
		lv.set("friction", friction);
		return lv;
	}
	
	public static int BYTE_SIZE = 17;
	
	Game game;
	
	private byte spriteIndex = 0;
	
	private float radius;
	
	private Vector2 position;
	private Vector2 velocity;
	private Vector2 acceleration;
	
	private float maxSpeed = -1;
	
	private float rotation;
	private float rotationalVelocity;
	private float rotationalAcceleration;
	
	private float maxRotationalVelocity = -1;
	
	private float mass;
	private float elasticity = 1.0f;
	private float friction = 0.0f;
	
	public void toByteBuffer(ByteBuffer data)
	{
		data.put(spriteIndex);
		data.putFloat(radius);
		data.putFloat(position.x);
		data.putFloat(position.y);
		data.putFloat(rotation);
	}
	
	public static GameObject fromByteBuffer(Game game, ByteBuffer data)
	{
		if(data.remaining() < BYTE_SIZE) return null;
		
		GameObject obj = new GameObject(game);
		obj.spriteIndex = data.get();
		obj.radius = data.getFloat();
		obj.position.x = data.getFloat();
		obj.position.y = data.getFloat();
		obj.rotation = data.getFloat();
		
		return obj;
	}
	
	GameObject(Game game)
	{
		this.game = game;
		setRadius(0);
		this.position = Vector2.zero();
		this.velocity = Vector2.zero();
		this.acceleration = Vector2.zero();
		this.rotation = 0;
		this.rotationalVelocity = 0;
		this.rotationalAcceleration = 0;
	}
	
	GameObject(Game game, float radius, Vector2 position)
	{
		this.game = game;
		setRadius(radius);
		this.position = new Vector2(position);
		this.velocity = Vector2.zero();
		this.acceleration = Vector2.zero();
		this.rotation = 0;
		this.rotationalVelocity = 0;
		this.rotationalAcceleration = 0;
	}
	
	
	public void setRadius(float radius)
	{
		this.radius = radius;
		this.mass = (float)Math.PI * radius * radius;
	}

	public void setSpriteIndex(byte i) { this.spriteIndex = i; }
	public void setPosition(Vector2 v) { this.position = new Vector2(v); }
	public void setVelocity(Vector2 v) { this.velocity = new Vector2(v); }
	public void setAcceleration(Vector2 v) { this.acceleration = new Vector2(v); }
	public void setMaxSpeed(float f) { this.maxSpeed = f; }
	public void setRotation(float f) { this.rotation = f; }
	public void setRotationalVelocity(float f) { this.rotationalVelocity = f; }
	public void setRotationalAcceleration(float f) { this.rotationalAcceleration = f; }
	public void setMaxRotationalVelocity(float f) { this.maxRotationalVelocity = f; }
	public void setElasticity(float f) { this.elasticity = f; }
	public void setFriction(float f) { this.friction = f; }
	
	public float getRadius() { return radius; }
	public Vector2 getPosition() { return new Vector2(position); }
	public Vector2 getVelocity() { return new Vector2(velocity); }
	public Vector2 getAcceleration() { return new Vector2(acceleration); }
	public float getMaxSpeed(float f) { return maxSpeed; }
	public float getRotation() { return rotation; }
	public float getRotationalVelocity() { return rotationalVelocity; }
	public float getRotationalAcceleration() { return rotationalAcceleration; }
	public float getMaxRotationalVelocity() { return maxRotationalVelocity; }
	public float getElasticity() { return elasticity; }
	public float getFriction() { return friction; }
	
	public void addForce(Vector2 force)
	{
		acceleration.add(force);
	}
	
	public void addTorque(float torque)
	{
		rotationalAcceleration += torque;
	}
	
	public void destroy()
	{
		game.objects.remove(this);
	}
	
	public float getMass()
	{
		return mass;
	}
	
	public void preUpdate()
	{
		if(game != null)
		{
			for(GameObject obj : game.objects)
			{
				if(!obj.position.equals(position))
				{
					Vector2 temp = new Vector2(obj.position);
					temp.subtract(position);
					float d = temp.getMagnitude();
					float m = getMass();
					float M = obj.getMass();
					
					float f = Game.G * m * M / d;
					temp.multiply(f);
					temp.divide(m);
					acceleration = temp;
				}
			}
		}
	}
	
	private void clampVelocity()
	{
		if(maxSpeed >= 0 && velocity.getMagnitude() > maxSpeed)
		{
			velocity.divide(velocity.getMagnitude());
			velocity.multiply(maxSpeed);
		}
	}
	
	private void clampRotationalVelocity()
	{
		if(maxRotationalVelocity >= 0)
		{
			if(rotationalVelocity > maxRotationalVelocity)
			{
				rotationalVelocity = maxRotationalVelocity;
			}
			else if(rotationalVelocity < -maxRotationalVelocity)
			{
				rotationalVelocity = -maxRotationalVelocity;
			}
		}
	}
	
	private void collideWithWalls()
	{
		if(position.y < Game.TOP + radius)
		{
			velocity.y *= -1;
			position.y = Game.TOP + radius;
		}
		if(position.y > Game.BOTTOM - radius)
		{
			velocity.y *= -1;
			position.y = Game.BOTTOM - radius;
		}
		if(position.x < Game.LEFT + radius)
		{
			velocity.x *= -1;
			position.x = Game.LEFT + radius;
		}
		if(position.x > Game.RIGHT - radius)
		{
			velocity.x *= -1;
			position.x = Game.RIGHT - radius;
		}
	}
	
	public void update()
	{
		velocity.add(acceleration);
		clampVelocity();
		position.add(velocity);
		velocity.multiply(1.0f - friction);
		
		rotationalVelocity += rotationalAcceleration;
		clampRotationalVelocity();
		rotation += rotationalVelocity;
		rotationalVelocity *= (1.0f - friction);
		
		collideWithWalls();
	}
	
	private void collideWithObject(GameObject obj)
	{
		Vector2 v1 = this.velocity;
		Vector2 v2 = obj.velocity;
		float m1 = this.mass;
		float m2 = obj.mass;
		
		this.velocity = v1.times(m1 - m2).plus(v2.times(2*m2)).dividedBy(m1 + m2);
		obj.velocity = v2.times(m2 - m1).plus(v1.times(2*m1)).dividedBy(m1 + m2);
		
		this.position.add(this.velocity);
		obj.position.add(obj.velocity);
		
		this.velocity.multiply(elasticity);
		obj.velocity.multiply(elasticity);
	}
	
	public void postUpdate()
	{
		for(int i = 0; i < game.objects.size(); i++)
		{
			GameObject obj = game.objects.get(i);
			
			if(obj != this)
			{
				Vector2 temp = obj.position.minus(position);
				float r = radius + obj.radius;
				if(temp.x*temp.x + temp.y*temp.y <= r*r)
				{
					collideWithObject(obj);
				}
			}
		}
	}
	
	public void draw(Graphics2D g)
	{
		BufferedImage sprite = Sprites.SPRITES[spriteIndex];
		if(sprite == null)
		{
			g.setColor(Color.LIGHT_GRAY);
			g.fillOval((int)(position.x - radius), (int)(position.y - radius), (int)(2 * radius), (int)(2 * radius));
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(3));
			g.drawOval((int)(position.x - radius), (int)(position.y - radius), (int)(2 * radius), (int)(2 * radius));
		}
		if(sprite != null)
		{
			int x = (int)(position.x);
			int y = (int)(position.y);
			
			int w = sprite.getWidth();
			int h = sprite.getHeight();
			
			AffineTransform trans = new AffineTransform();
			trans.translate(x, y);
			trans.rotate(rotation);
			trans.scale(2 * radius / w, 2 * radius / h);
			trans.translate(-sprite.getWidth()/2, -sprite.getHeight()/2);
			
			g.drawImage(sprite, trans, null);
		}
	}
}
