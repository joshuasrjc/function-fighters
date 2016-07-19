package com.github.joshuasrjc.functionfighters.game;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class Vector2
{
	public static LuaValue toGlobalLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		lv.set("new", luaNew);
		return lv;
	}
	
	public LuaValue toLuaValue()
	{
		LuaValue lv = LuaValue.tableOf();
		LuaValue mt = LuaValue.tableOf();
		lv.set("x", x);
		lv.set("y", y);

		mt.set("__unm", unm);
		mt.set("__add", add);
		mt.set("__sub", sub);
		mt.set("__mul", mul);
		mt.set("__div", div);
		mt.set("__eq", eq);
		lv.setmetatable(mt);
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
	
	Vector2(float theta)
	{
		this.x = (float)Math.cos(theta);
		this.y = (float)Math.sin(theta);
	}
	
	Vector2(LuaValue arg0, LuaValue arg1) throws LuaError
	{
		LuaValue xlv = arg0;
		LuaValue ylv = arg1;
		if(arg0.istable())
		{
			LuaValue vector = arg0.get("position");
			if(vector.isnil())
			{
				vector = arg0;
			}
			if(vector.istable())
			{
				xlv = vector.get("x");
				ylv = vector.get("y");
			}
		}
		if(xlv.isnumber() && ylv.isnumber())
		{
			this.x = xlv.tofloat();
			this.y = ylv.tofloat();
		}
		else
		{
			this.x = Float.NaN;
			this.y = Float.NaN;
			if(arg0.isnil() || (!arg0.istable() && arg1.isnil()))
			{
				throw new LuaError("object or position expected, got nil");
			}
			else
			{
				throw new LuaError("object or position excpected");
			}
		}
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
	
	public String toString()
	{
		return String.format("(%.2f, %.2f)", x, y);
	}
	
	private static LuaValue luaNew = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		arg0.checknumber();
		arg1.checknumber();
		Vector2 v = new Vector2(arg0.tofloat(), arg1.tofloat());
		return v.toLuaValue();
	}};

	private LuaValue unm = new OneArgFunction() { @Override public LuaValue call(LuaValue arg0)
	{
		arg0.checktable();
		LuaValue lvx0 = arg0.get("x");
		LuaValue lvy0 = arg0.get("y");
		lvx0.checknumber();
		lvy0.checknumber();
		
		float x0 = lvx0.tofloat();
		float y0 = lvy0.tofloat();
		
		return new Vector2(-x0, -y0).toLuaValue();
	}};

	
	private LuaValue add = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		arg0.checktable();
		arg1.checktable();
		LuaValue lvx0 = arg0.get("x");
		LuaValue lvy0 = arg0.get("y");
		LuaValue lvx1 = arg1.get("x");
		LuaValue lvy1 = arg1.get("y");
		lvx0.checknumber();
		lvy0.checknumber();
		lvx1.checknumber();
		lvy1.checknumber();
		
		float x0 = lvx0.tofloat();
		float y0 = lvy0.tofloat();
		float x1 = lvx1.tofloat();
		float y1 = lvy1.tofloat();
		
		return new Vector2(x0 + x1, y0 + y1).toLuaValue();
	}};

	
	private LuaValue sub = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		arg0.checktable();
		arg1.checktable();
		LuaValue lvx0 = arg0.get("x");
		LuaValue lvy0 = arg0.get("y");
		LuaValue lvx1 = arg1.get("x");
		LuaValue lvy1 = arg1.get("y");
		lvx0.checknumber();
		lvy0.checknumber();
		lvx1.checknumber();
		lvy1.checknumber();
		
		float x0 = lvx0.tofloat();
		float y0 = lvy0.tofloat();
		float x1 = lvx1.tofloat();
		float y1 = lvy1.tofloat();
		
		return new Vector2(x0 - x1, y0 - y1).toLuaValue();
	}};

	private LuaValue mul = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		LuaValue v = arg0;
		LuaValue n = arg1;
		if(n.istable())
		{
			v = arg1;
			n = arg0;
		}
		v.checktable();
		n.checknumber();
		
		LuaValue lvx = v.get("x");
		LuaValue lvy = v.get("y");
		lvx.checknumber();
		lvy.checknumber();
		
		float a = n.tofloat();
		float x = lvx.tofloat();
		float y = lvy.tofloat();
		
		return new Vector2(a*x, a*y).toLuaValue();
	}};

	private LuaValue div = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		LuaValue v = arg0;
		LuaValue n = arg1;
		v.checktable();
		n.checknumber();
		
		LuaValue lvx = v.get("x");
		LuaValue lvy = v.get("y");
		lvx.checknumber();
		lvy.checknumber();
		
		float a = n.tofloat();
		float x = lvx.tofloat();
		float y = lvy.tofloat();
		
		return new Vector2(x/a, y/a).toLuaValue();
	}};

	private LuaValue eq = new TwoArgFunction() { @Override public LuaValue call(LuaValue arg0, LuaValue arg1)
	{
		arg0.checktable();
		arg1.checktable();
		LuaValue lvx0 = arg0.get("x");
		LuaValue lvy0 = arg0.get("y");
		LuaValue lvx1 = arg1.get("x");
		LuaValue lvy1 = arg1.get("y");
		lvx0.checknumber();
		lvy0.checknumber();
		lvx1.checknumber();
		lvy1.checknumber();
		
		float x0 = lvx0.tofloat();
		float y0 = lvy0.tofloat();
		float x1 = lvx1.tofloat();
		float y1 = lvy1.tofloat();
		
		return LuaValue.valueOf(x0 == x1 && y0 == y1);
	}};
}
