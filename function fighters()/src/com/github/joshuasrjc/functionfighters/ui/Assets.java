package com.github.joshuasrjc.functionfighters.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.github.joshuasrjc.functionfighters.game.Game;

public class Assets
{
	public static final int SOUND_SHOOT_ID = 0;
	public static final int SOUND_HIT_ID = 1;
	public static final int SOUND_EXPLOSION_ID = 2;
	
	public static BufferedImage bulletSprite;
	public static BufferedImage[] fighterSprites = new BufferedImage[Game.N_TEAMS];
	public static final String[] soundNames = 
	{
			"shoot.wav",
			"hit.wav",
			"explosion.wav"
	};
	
	public static void loadAssets()
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		
		try
		{
			bulletSprite = ImageIO.read(classLoader.getResourceAsStream("bullet.png"));
		}
		catch(Exception ex)
		{
			ChatLog.logError("Error loading bullet.png");
		}
		
		for(int i = 0; i < fighterSprites.length; i++)
		{
			String filepath = "fighter" + i + ".png";
			try
			{
				fighterSprites[i] = ImageIO.read(classLoader.getResourceAsStream(filepath));
			}
			catch(Exception ex)
			{
				ChatLog.logError("Error loading " + filepath);
			}
		}
	}
	
	public static void playSound(int id)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
					AudioInputStream in = AudioSystem.getAudioInputStream(classLoader.getResourceAsStream(soundNames[id]));
					Clip clip = AudioSystem.getClip();
					clip.open(in);
					clip.start();
				}
				catch(Exception e)
				{
					ChatLog.logError("Error playing audio file " + soundNames[id]);
				}
			}
		}).start();
	}
}
