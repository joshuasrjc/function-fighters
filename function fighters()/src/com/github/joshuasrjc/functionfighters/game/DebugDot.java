package com.github.joshuasrjc.functionfighters.game;

public class DebugDot extends GameObject
{
	DebugDot(Game game, Vector2 position)
	{
		super(game, 2, position);
	}

	@Override
	public void update(int step)
	{
		if(step == Game.DEBUG_STEP)
		{
			destroy();
		}
	}
}
