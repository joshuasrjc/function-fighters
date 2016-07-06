package com.github.joshuasrjc.functionfighters.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Sprites
{
	public static final BufferedImage[] SPRITES = new BufferedImage[256];
	
	private static final String[] FILE_NAMES = 
	{
		"",
		"fighter.png"	
	};
	
	public static void loadSprites()
	{
		for(int i = 1; i < FILE_NAMES.length; i++)
		{
			try
			{
				SPRITES[i] = ImageIO.read(new File(FILE_NAMES[i]));
			}
			catch(IOException ex)
			{
				ChatLog.logError("Error loading sprite: " + FILE_NAMES[i]);
			}
		}
	}
}
