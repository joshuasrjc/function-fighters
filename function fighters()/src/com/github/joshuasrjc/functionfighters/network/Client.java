package com.github.joshuasrjc.functionfighters.network;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.github.joshuasrjc.functionfighters.game.GameObject;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;

public class Client
{
	private Client client = this;
	private boolean connected = true;
	private boolean host = false;
	private Server server;
	private String nickname = "";
	
	private Thread thread;
	private Socket socket;
	private BufferedIO io;
	
	private String script = "";
	private boolean scriptLoaded = false;
	
	Client(Socket socket, Server server) throws IOException
	{
		this.socket = socket;
		this.server = server;
		io = new BufferedIO(socket);
		
		thread = new Thread(new Runnable()
		{
			
			// <Separate Thread> //
			@Override
			public void run()
			{
				try
				{
					Packet loginPacket = io.readPacket();
					if(loginPacket.type == Packet.LOGIN)
					{
						String nickname = loginPacket.getFirstLine();
						String password = loginPacket.getAllButFirstLine();
						if(!server.isValidNickname(nickname))
						{
							sendPacket(new Packet(Packet.BAD_NICKNAME));
							disconnect();
						}
						if(!server.matchesPassword(password))
						{
							sendPacket(new Packet(Packet.BAD_PASSWORD));
							disconnect();
						}
						else
						{
							client.nickname = nickname;
							sendPacket(new Packet(Packet.ACCEPTED));
							server.onClientConnected(client);
							while(connected)
							{
								Packet packet = io.readPacket();
								server.onClientSentPacket(client, packet);
							}
						}
					}
				}
				catch (IOException e) {}
				if(connected) disconnect();
			}
			// </Separate Thread> //
			
		});
		thread.start();
	}
	
	public void sendPacket(Packet packet)
	{
		try
		{
			io.writePacket(packet);
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach client.");
			disconnect();
		}
	}
	
	public void disconnect()
	{
		connected = false;
		server.removeClient(this);
		
		try
		{
			io.close();
			socket.close();
		}
		catch(IOException ex)
		{
			ChatLog.logError("Error disconnecting client.");
		}

		ChatLog.logInfo("Player [" + nickname + "] has disconnected from the server."); 
	}
	
	public String getNickname()
	{
		return new String(nickname);
	}
	
	public String getScript()
	{
		return script;
	}
	
	public void makeHost()
	{
		host = true;
	}
	
	public boolean isHost()
	{
		return host;
	}
}
