package com.github.joshuasrjc.functionfighters.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Sprites
{
	public static BufferedImage fighterSprite;
	
	public static void loadSprites()
	{
		try { fighterSprite = ImageIO.read(new File("fighter.png")); }
		catch(IOException ex) { ChatLog.logError("Error loading fighter sprite."); }
	}
}
