package com.github.joshuasrjc.functionfighters.ui;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;

import com.github.joshuasrjc.functionfighters.game.Game;

import sun.audio.*;
import com.sun.media.sound.*;

import javazoom.jl.player.*;
import javazoom.jl.player.advanced.AdvancedPlayer;

public class Assets
{
	public static final int SOUND_SHOOT_ID = 0;
	public static final int SOUND_HIT_ID = 1;
	public static final int SOUND_EXPLOSION_ID = 2;
	public static final int SOUND_ERROR_ID = 3;
	
	public static ImageIcon icon;
	public static BufferedImage bulletSprite;
	public static BufferedImage[] fighterSprites = new BufferedImage[Game.N_TEAMS];
	public static final String[] SOUND_NAMES = 
	{
		"shoot.wav",
		"hit.wav",
		"explosion.wav",
		"error.wav"
	};
	public static final String[] SONG_NAMES = 
	{
		"Mind_Over_Matter.mp3",
		"Weird_Electro.mp3",
		"Septic_Mind.mp3"
	};
	
	private static AdvancedPlayer musicPlayer = null;
	private static boolean musicMuted = false;
	private static boolean soundMuted = false;
	
	public static void loadAssets()
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		
		try
		{
			icon = new ImageIcon(classLoader.getResource("icon.png"));
		}
		catch(Exception ex)
		{
			ChatLog.logError("Error loading icon.png");
		}
		
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
		
		soundMuted = FileCache.getBool(FileCache.SOUND_MUTED, false);
		musicMuted = FileCache.getBool(FileCache.MUSIC_MUTED, false);
	}
	
	public static void playSound(int id)
	{
		if(soundMuted) return;
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
					AudioInputStream in = AudioSystem.getAudioInputStream(classLoader.getResource(SOUND_NAMES[id]));
					Clip clip = AudioSystem.getClip();
					clip.open(in);
					clip.start();
					while(clip.isRunning())
					{
						try{ Thread.sleep(100); } finally {}
					}
					in.close();
				}
				catch(Exception e)
				{
					ChatLog.logError("Error playing audio file " + SOUND_NAMES[id]);
				}
			}
		}).start();
	}
	
	public static void startMusic()
	{
		if(musicPlayer != null)
		{
			musicPlayer.close();
		}
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				int songID = new Random().nextInt(SONG_NAMES.length);
				songID = 0;
				while(!musicMuted)
				{
					try
					{
						ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
						BufferedInputStream in = new BufferedInputStream(classLoader.getResourceAsStream(SONG_NAMES[songID]));
						musicPlayer = new AdvancedPlayer(in);
						musicPlayer.play();
						songID++;
						if(songID >= SONG_NAMES.length) songID = 0;
					}
					catch(Exception ex)
					{
						ChatLog.logError("Error playing audio file " + SONG_NAMES[songID]);
						break;
					}
					try{ Thread.sleep(2000); } catch(Exception ex) {}
				}
			}
		}).start();
	}
	
	public static void toggleMuteSound()
	{
		soundMuted = !soundMuted;
		FileCache.cacheBool(FileCache.SOUND_MUTED, soundMuted);
	}
	
	public static void toggleMuteMusic()
	{
		musicMuted = !musicMuted;
		if(musicMuted && musicPlayer != null)
		{
			musicPlayer.close();
		}
		if(!musicMuted)
		{
			startMusic();
		}
		FileCache.cacheBool(FileCache.MUSIC_MUTED, musicMuted);
	}
}
