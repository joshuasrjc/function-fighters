package com.github.joshuasrjc.functionfighters.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FileCache
{
	public static String NICKNAME = "nickname";
	public static String ADDRESS = "address";
	public static String PORT = "port";
	
	private static File dir;
	private static File cache;
	
	public static void initCache()
	{
		dir = new File(System.getProperty("user.home") + "/.functionfighters");
		
		if(dir.exists() && !dir.isDirectory())
		{
			ChatLog.logError("Error opening cache directory.");
			return;
		}
		
		if(!dir.exists())
		{
			dir.mkdir();
		}
		
		cache = new File(dir, "settings.ini");
		if(cache.exists() && !cache.isFile())
		{
			ChatLog.logError("Error opening cache file.");
			return;
		}
		
		if(!cache.exists())
		{
			try
			{
				cache.createNewFile();
			} catch (IOException ex)
			{
				ChatLog.logError("Error creating cache file.");
			}
		}
	}
	
	public static void cacheString(String key, String value)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
			String line;
			String text = "";
			
			boolean cached = false;
			
			while((line = in.readLine()) != null)
			{
				if(line.startsWith(key + '='))
				{
					line = line.substring(0, line.indexOf('=') + 1) + value + '\n';
					cached = true;
				}
				
				if(!line.equals("")) text += line + '\n';
			}
			
			if(!cached)
			{
				text += key + "=" + value + '\n';
			}
			
			in.close();
			BufferedWriter out = new BufferedWriter(new PrintWriter(new FileOutputStream(cache)));
			
			out.write(text);
			
			out.flush();
			out.close();
		}
		catch (IOException ex)
		{
			ChatLog.logError("Error caching value.");
		}
	}
	
	public static String getString(String key)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
			String line;
			
			while((line = in.readLine()) != null)
			{
				if(line.startsWith(key + '='))
				{
					if(line.length() > key.length() + 1)
					{
						String value = line.substring(line.indexOf('=') + 1);
						in.close();
						return value;
					}
				}
			}
			
			in.close();
		}
		catch (IOException ex)
		{
			ChatLog.logError("Error reading value.");
		}
		
		
		return null;
	}
	
	public static void cacheInt(String key, int value)
	{
		cacheString(key, "" + value);
	}
	
	public static Integer getInt(String key)
	{
		String str = getString(key);
		if(str == null) return null;
		
		try
		{
			int value = Integer.parseInt(str);
			return value;
		}
		catch(Exception ex)
		{
			ChatLog.logError("Error parsing number value.");
		}
		
		return null;
	}
}
