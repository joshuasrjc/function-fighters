package com.github.joshuasrjc.functionfighters.game;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class GameEvent
{
	public int type;
	public Varargs args;
	
	GameEvent(int type)
	{
		this.type = type;
	}
	
	GameEvent(int type, Varargs args)
	{
		this.type = type;
		this.args = args;
	}
}
