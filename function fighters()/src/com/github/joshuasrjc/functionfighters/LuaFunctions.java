package com.github.joshuasrjc.functionfighters;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;

public class LuaFunctions
{
	public static final LuaValue print(Server server)
	{
		return new OneArgFunction()
		{
			@Override
			public LuaValue call(LuaValue str)
			{
				server.sendMessageToAllClients(Server.INFO, str.tojstring());
				return NIL;
			}
		};
	}
}
