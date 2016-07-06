package com.github.joshuasrjc.functionfighters.network;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

public class Commands
{
	Server server;
	
	Commands(Server server)
	{
		this.server = server;
	}
	
	public void executeCommand(Client client, String message)
	{
		int i = message.indexOf(' ');
		if(i < 0)
		{
			i = message.length();
		}
		
		String command = message.substring(1, i);
		
		message = message.substring(i);
		if(message.startsWith(" "))
		{
			message = message.substring(1);
		}

		System.out.println("[" + command + "]");
		System.out.println("[" + message + "]");
		
		if(command.equals("script"))
		{
			script(client, message);
		}
		else if(command.equals("kick"))
		{
			kick(client, message);
		}
	}
	
	public void script(Client client, String message)
	{
		Globals globals = JsePlatform.standardGlobals();
		LuaValue print = CoerceJavaToLua.coerce(new OneArgFunction()
		{
			@Override
			public LuaValue call(LuaValue arg)
			{
				server.sendMessageToAllClients(Server.INFO, arg.tojstring());
				return NIL;
			}
		});
		globals.set("print", print);
		try
		{
			if(message.equals(""))
			{
				message = client.getScript();
			}
			else
			{
				server.sendMessageToAllClients(Server.CODE, message);
			}
			LuaValue chunk = globals.load(message);
			chunk.call();
		}
		catch(Exception e)
		{
			server.sendMessageToAllClients(Server.ERROR, "Error: Invalid script.");
		}
	}
	
	public void kick(Client client, String message)
	{
		if(client.isHost()) server.kickClient(message);
	}
}
