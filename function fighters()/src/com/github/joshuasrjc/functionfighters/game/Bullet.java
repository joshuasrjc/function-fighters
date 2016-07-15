package com.github.joshuasrjc.functionfighters.game;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.github.joshuasrjc.functionfighters.ui.Assets;

public class Bullet extends GameObject
{
	public static final float BULLET_RADIUS = 6;
	
	private int team;
	private int shooterID;
	private BufferedImage sprite = Assets.bulletSprite;
	
	private float damage;
	private RayCastFilter filter = new RayCastFilter()
	{
		@Override
		public boolean doTest(GameObject obj)
		{
			return obj instanceof Fighter && ((Fighter)obj).team != team;
		}
		
	};
	
	Bullet(Game game, Vector2 position, Vector2 velocity, float rotation, float damage, int team, int shooterID)
	{
		super(game, BULLET_RADIUS, position);
		this.setVelocity(velocity);
		this.setRotation(rotation);
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
		if(obj instanceof Fighter)
		{
			Fighter fighter = (Fighter)obj;
			if(fighter.team != team)
			{
				fighter.dealDamage(damage);
			}
		}
		destroy();
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
	}
}
