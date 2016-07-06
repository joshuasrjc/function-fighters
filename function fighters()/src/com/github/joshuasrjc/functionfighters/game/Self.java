package com.github.joshuasrjc.functionfighters.game;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.github.joshuasrjc.functionfighters.LuaFunctions;
import com.github.joshuasrjc.functionfighters.network.Server;

public class Self extends Fighter
{
	public LuaValue toLuaValue()
	{
		LuaValue lv = super.toLuaValue();
		lv.set("moveForward", moveForward);
		lv.set("moveRight", moveRight);
		lv.set("moveBackward", moveBackward);
		lv.set("moveLeft", moveLeft);
		return lv;
	}
	
	public static final float QUARTER_TURN = (float)Math.PI / 2f;
	
	Self self = this;
	String script;
	Server server;
	Globals globals;
	LuaValue update;
	
	Self(Game game, Vector2 position, String script, Server server)
	{
		super(game, position);
		
		this.script = script;
		this.server = server;
		globals = JsePlatform.standardGlobals();
		globals.set("print", LuaFunctions.print(server));
		LuaValue luaScript = globals.load(script);
		luaScript.call();
		update = globals.get("update");
	}
	
	@Override
	public void preUpdate()
	{
		this.setAcceleration(Vector2.zero());
		this.setRotationalAcceleration(0);
		
		if(update.isfunction())
		{
			update.call(toLuaValue());
		}
		
		//addForce(new Vector2(1, 0));
		
		this.setAcceleration(this.getAcceleration().normalized().times(MOVEMENT_THRUST));
		if(this.getRotationalAcceleration() > 1) this.setRotationalAcceleration(1f);
		if(this.getRotationalAcceleration() < -1) this.setRotationalAcceleration(-1f);
		this.setRotationalAcceleration(this.getRotationalAcceleration() * TURNING_THRUST);
	}

	public void moveInDirection(float theta, float speed)
	{
		addForce(new Vector2(speed * (float)Math.cos(theta), speed * (float)Math.sin(theta)));
	}
	
	LuaValue moveForward = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation(), speed.tofloat());
		else self.moveInDirection(self.getRotation(), 1f);
		return NIL;
	}};
	
	LuaValue moveRight = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + QUARTER_TURN, 1f);
		return NIL;
	}};
	
	LuaValue moveBackward = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + 2 * QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + 2 * QUARTER_TURN, 1f);
		return NIL;
	}};
	
	LuaValue moveLeft = new OneArgFunction() { @Override public LuaValue call(LuaValue speed)
	{
		if(speed.isnumber()) self.moveInDirection(self.getRotation() + 3 * QUARTER_TURN, speed.tofloat());
		else self.moveInDirection(self.getRotation() + 3 * QUARTER_TURN, 1f);
		return NIL;
	}};
}
