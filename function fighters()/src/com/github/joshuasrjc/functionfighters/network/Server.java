package com.github.joshuasrjc.functionfighters.network;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.game.GameObject;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;

public class Server
{
	public static final long CLIENT_ACCEPT_TIMEOUT_MILLIS = 500;
	
	public static int unsignByte(byte b)
	{
		return (int)(b & 0xFF);
	}
	
	public static byte signByte(int i)
	{
		return (byte)(i);
	}
	
	// <Server Objects> //
	private Server server = this;
	private boolean hosting = false;
	private String password = "";
	private ServerSocket serverSocket;
	private Thread serverThread;
	private ArrayList<Client> clients = new ArrayList<Client>();
	private ArrayList<ClientListener> clientListeners = new ArrayList<ClientListener>();
	private Commands commands = new Commands(this);
	// </Server Objects> //
	
	
	
	
	
	
	
	
	// <Client Objects> //
	private boolean connected = false;
	private Socket clientSocket;
	private Thread clientThread;
	private BufferedIO io;
	private ArrayList<ServerListener> serverListeners = new ArrayList<ServerListener>();
	// </Client Objects> //
	
	
	
	
	
	
	
	// Constructor //
	public Server()
	{
		
	}
	
	
	
	
	
	
	
	
	// <Server Methods> //
	
	public void startHosting(Game game, int port, String password)
	{
		if(hosting) stopHosting();
		
		ChatLog.logInfo("Starting server on port: " + port);
		
		this.password = password;
		
		try
		{
			serverSocket = new ServerSocket(port);
			hosting = true;
			onServerStart();
			
			serverThread = new Thread(new Runnable()
			{
				
				// <Separate Thread> //
				@Override
				public void run()
				{
					while(hosting)
					{
						try
						{
							Client client = new Client(serverSocket.accept(), server);
							if(clients.size() == 0) client.makeHost();
						}
						catch(IOException ex)
						{
							//ChatLog.logError("Error accepting client.");
						}
						try { Thread.sleep(CLIENT_ACCEPT_TIMEOUT_MILLIS); } catch(Exception e) {};
					}
				}
				// </Separate Thread> //
				
			});
			serverThread.start();
		}
		catch(IOException ex)
		{
			ChatLog.logError("Unable to start server.");
			stopHosting();
		}
	}
	
	private void onServerStart()
	{
		for(ServerListener sl : serverListeners)
		{
			sl.onServerStart();
		}
	}
	
	private void onServerStop()
	{
		for(ServerListener sl : serverListeners)
		{
			sl.onServerStop();
		}
	}
	
	public void onClientConnected(Client client)
	{
		clients.add(client);
		for(ServerListener sl : serverListeners)
		{
			sl.onClientConnected(client);
		}
	}
	
	public void onClientDisconnected(Client client)
	{
		for(ServerListener sl : serverListeners)
		{
			sl.onClientDisconnected(client);
		}
		clients.remove(client);
	}
	
	public void onClientSentPacket(Client client, Packet packet)
	{
		for(ServerListener sl : serverListeners)
		{
			sl.onClientSentPacket(client, packet);
		}
	}
	
	public void sendPacketToAllClients(Packet packet)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			client.sendPacket(packet);
		}
	}
	
	public void kickClient(String nickname)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			if(client.getNickname().toLowerCase().equals(nickname.toLowerCase()))
			{
				client.sendPacket(new Packet(Packet.INFO, "You have been kicked."));
				client.disconnect();
			}
		}
	}
	
	public void addClientListener(ClientListener listener)
	{
		clientListeners.add(listener);
	}
	
	public void removeClient(Client client)
	{
		clients.remove(client);
	}
	
	public boolean isHosting()
	{
		return hosting;
	}
	
	public boolean matchesPassword(String password)
	{
		return this.password.equals(password);
	}
	
	public boolean isValidNickname(String nickname)
	{
		for(Client client : clients)
		{
			if(client.getNickname().toLowerCase().equals(nickname.toLowerCase()))
			{
				return false;
			}
		}
		
		if(nickname.equals("")) return false;
		
		return true;
	}
	
	public void stopHosting()
	{
		hosting = false;
		onServerStop();
		
		while(clients.size() > 0)
		{
			clients.get(0).disconnect();
		}
		
		try
		{
			serverSocket.close();
			ChatLog.logInfo("Closed Server");
		}
		catch (Exception e)
		{
			ChatLog.logError("Error closing server.");
		}
	}
	
	// </Server Methods> //
	//  _________
	// |         |
	// | Server  |
	// |         |
	// |_________|
	//    |
	//    |___
	//    |WWW|
	//        |
	//        |
	//  ______|__
	// |         |
	// | Client  |
	// |         |
	// |_________|
	// <Client Methods> //
	
	public void connectToServer(String address, int port, String nickname, String password)
	{
		try
		{
			clientSocket = new Socket(address, port);
			io = new BufferedIO(clientSocket);
			
			Packet loginPacket = new Packet(Packet.LOGIN, nickname + '\n' + password);
			io.writePacket(loginPacket);
			
			Packet packet = io.readPacket();
			if(packet.type == Packet.BAD_PASSWORD)
			{
				ChatLog.logError("Incorrect Password");
				throw new IOException();
			}
			if(packet.type == Packet.BAD_NICKNAME)
			{
				ChatLog.logError("That nickname is invalid, or is already in use.");
				throw new IOException();
			}
			if(packet.type != Packet.ACCEPTED)
			{
				throw new IOException();
			}
		}
		catch(IOException ex)
		{
			ChatLog.logError("Unable to connect to server");
			return;
		}
		
		connected = true;
		onConnectToServer();
		
		clientThread = new Thread(new Runnable()
		{
			
			// <Separate Thread> //
			@Override
			public void run()
			{
				try
				{
					while(connected)
					{
						Packet packet = io.readPacket();
						onServerSentPacket(packet);
					}
				}
				catch(IOException ex)
				{
					disconnectFromServer();
				}
			}
			// </Separate Thread> //
			
		});
		clientThread.start();
	}
	
	public void sendPacketToServer(Packet packet)
	{
		try
		{
			io.writePacket(packet);
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach server.");
			disconnectFromServer();
		}
	}
	
	private void onConnectToServer()
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onConnectToServer();
		}
	}
	
	private void onDisconnectFromServer()
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onDisconnectFromServer();
		}
	}
	
	private void onServerSentPacket(Packet packet)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentPacket(packet);
		}
	}
	
	public void addServerListener(ServerListener listener)
	{
		serverListeners.add(listener);
	}
	
	public void disconnectFromServer()
	{
		if(connected) onDisconnectFromServer();
		connected = false;
		
		try
		{
			io.close();;
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ChatLog.logError("Error closing connection to server.");
		}
		
	}
	
	// </Client Methods> //
}
