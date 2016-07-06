package com.github.joshuasrjc.functionfighters.game;

import org.luaj.vm2.LuaValue;


public class Fighter extends GameObject
{
	public static final float MOVEMENT_THRUST = 1f;
	public static final float TURNING_THRUST = 0.01f;
	
	@Override
	public LuaValue toLuaValue()
	{
		LuaValue lv = super.toLuaValue();
		lv.set("movementThrust", MOVEMENT_THRUST);
		lv.set("turningThrust", TURNING_THRUST);
		return lv;
	}

	Fighter(Game game, Vector2 position)
	{
		super(game, 16, position);

		this.setSpriteIndex((byte)1);
		this.setElasticity(0.9f);
		this.setFriction(0.05f);
		this.setMaxSpeed(5f);
	}
}
