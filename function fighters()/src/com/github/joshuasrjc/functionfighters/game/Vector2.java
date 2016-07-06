package com.github.joshuasrjc.functionfighters.game;

import org.luaj.vm2.LuaValue;

public class Vector2
{
	public LuaValue toLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		lv.set("x", x);
		lv.set("y", y);
		return lv;
	}
	
	public static Vector2 zero()
	{
		return new Vector2(0, 0);
	}
	
	public static Vector2 randomInCircle(float radius)
	{
		radius =  radius * (float)Math.sqrt(Math.random());
		double theta = Math.random() * 2 * Math.PI;
		return new Vector2(radius * (float)Math.cos(theta), radius * (float)Math.sin(theta));
	}
	
	public float x;
	public float y;
	
	Vector2(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	Vector2(Vector2 v)
	{
		this.x = v.x;
		this.y = v.y;
	}
	
	@Override
	public boolean equals(Object o)
	{
		try
		{
			Vector2 v = (Vector2)o;
			return x == v.x && y == v.y;
		}
		catch(ClassCastException ex)
		{
			return false;
		}
	}
	
	public void add(Vector2 v)
	{
		x += v.x;
		y += v.y;
	}
	
	public void subtract(Vector2 v)
	{
		x -= v.x;
		y -= v.y;
	}
	
	public void multiply(float f)
	{
		x *= f;
		y *= f;
	}
	
	public void divide(float f)
	{
		x /= f;
		y /= f;
	}
	
	public Vector2 plus(Vector2 v)
	{
		return new Vector2(x + v.x, y + v.y);
	}
	
	public Vector2 minus(Vector2 v)
	{
		return new Vector2(x - v.x, y - v.y);
	}
	
	public Vector2 times(float f)
	{
		return new Vector2(x * f, y * f);
	}
	
	public Vector2 dividedBy(float f)
	{
		return new Vector2(x / f, y / f);
	}
	
	public float dot(Vector2 v)
	{
		return x*v.x + y*v.y;
	}
	
	public float getMagnitude()
	{
		return (float)Math.sqrt(x*x + y*y);
	}
	
	public void normalize()
	{
		float mag = getMagnitude();
		if(mag > 0) divide(mag);
	}
	
	public Vector2 normalized()
	{
		float mag = getMagnitude();
		if(mag > 0) return this.dividedBy(mag);
		else return zero();
	}
}
