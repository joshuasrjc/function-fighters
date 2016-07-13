package com.github.joshuasrjc.functionfighters.game;

import java.nio.ByteBuffer;

public class Bullet extends GameObject
{
	public static final float BULLET_RADIUS = 3;
	
	private int team;
	private int shooterID;
	
	private float damage;
	private RayCastFilter filter = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj instanceof Fighter && ((Fighter)obj).team != team;
		}
		
	};
	
	Bullet(Game game, Vector2 position, Vector2 velocity, float damage, int team, int shooterID)
	{
		super(game, BULLET_RADIUS, position);
		this.setVelocity(velocity);
		this.damage = damage;
		this.team = team;
		this.shooterID = shooterID;
		this.setCanCollide(false);
	}
	
	public Bullet(ByteBuffer data)
	{
		super(data);
	}
	
	@Override
	public void onCollide(GameObject obj)
	{
		if(obj == null)
		{
			destroy();
		}
	}
	
	@Override
	public void update()
	{
		Fighter fighter = (Fighter)game.castRay(position, velocity, getRadius(), filter);
		if(fighter != null)
		{
			fighter.dealDamage(damage);
			destroy();
		}
		super.update();
	}
}
