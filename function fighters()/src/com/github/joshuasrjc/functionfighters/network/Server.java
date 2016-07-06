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
	public static final byte INFO = 0x00;
	public static final byte CHAT = 0x01;
	public static final byte ERROR = 0x02;
	public static final byte CODE = 0x03;
	
	public static final byte PASSWORD = 0x10;
	public static final byte NICKNAME = 0x11;
	
	public static final byte ACCEPTED = 0x20;
	public static final byte DENIED = 0x21;
	public static final byte BAD_PASSWORD = 0x22;
	public static final byte BAD_NICKNAME = 0x23;
	
	public static final byte FRAME = 0x30;
	public static final byte SCRIPT = 0x31;
	public static final byte GAME_START = 0x32;
	public static final byte GAME_PAUSE = 0x33;
	public static final byte GAME_STOP = 0x34;
	
	public static final byte ITEM_SELECT = 0x40;
	public static final byte ITEM_ADD = 0x41;
	public static final byte ITEM_REMOVE = 0x42;
	
	public static final int MAX_MESSAGE_LENGTH = 14000;
	public static final long CLIENT_MESSAGE_DELAY_MILLIS = 100;
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
	private ArrayList<String> scripts = new ArrayList<String>();
	private ArrayList<String> scriptNames = new ArrayList<String>();
	private int[] selections = new int[256];
	private Game game;
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
		for(int i = 0; i < selections.length; i++)
		{
			selections[i] = -1;
		}
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
							clients.add(client);
							
							//try { Thread.sleep(CLIENT_MESSAGE_DELAY_MILLIS); } catch(Exception e) {};
							
							if(game.isRunning())
							{
								client.sendByte(Server.GAME_START);
							}
							if(game.isPaused())
							{
								client.sendByte(Server.GAME_START);
							}
							for(int i = 0; i < scriptNames.size(); i++)
							{
								String name = scriptNames.get(i);
								client.sendMessage(Server.ITEM_ADD, name);
							}
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
	
	public void onClientSentMessage(Client client, String message, byte type)
	{
		for(ServerListener sl : serverListeners)
		{
			sl.onClientSentMessage(client, message);
		}
		
		if(message.length() > 0)
		{
			if(message.charAt(0) == '/')
			{
				commands.executeCommand(client, message);
			}
			else
			{
				sendMessageToAllClients(type, client.getNickname() + ": " + message);
			}
		}
	}
	
	public void onClientSentScript(Client client, String name, String text)
	{
		if(scripts.size() < 256)
		{
			scripts.add(text);
			scriptNames.add(name);
			sendMessageToAllClients(Server.ITEM_ADD, name);
		}
	}
	
	public void onClientSentItemRemoval(int index)
	{
		if(index < scripts.size())
		{
			scripts.remove(index);
			scriptNames.remove(index);
			sendItemRemovalToAllClients(index);
		}
	}
	
	public void onClientSentItemSelection(int index, int team)
	{
		if(index < scripts.size())
		{
			selections[team] = index;
			sendItemSelectionToAllClients(index, team);
		}
	}
	
	public void onClientSentByte(Client client, byte b)
	{
		for(ServerListener listener : serverListeners)
		{
			listener.onClientSentByte(client, b);
		}
	}
	
	public void sendMessageToAllClients(byte type, String message)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			client.sendMessage(type, message);
		}
	}
	
	public void sendFrameToAllClients(Frame frame)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			client.sendFrame(frame);
		}
	}
	
	public void sendItemRemovalToAllClients(int index)
	{
		
	}
	
	public void sendItemSelectionToAllClients(int index, int team)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			client.sendItemSelection(index, team);
		}
	}
	
	public void sendByteToAllClients(byte b)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			client.sendByte(b);
		}
	}
	
	public void kickClient(String nickname)
	{
		for(int i = 0; i < clients.size(); i++)
		{
			Client client = clients.get(i);
			if(client.getNickname().toLowerCase().equals(nickname.toLowerCase()))
			{
				client.sendMessage(Server.INFO, "You have been kicked.");
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
	
	public boolean isNicknameTaken(String nickname)
	{
		for(Client client : clients)
		{
			if(client.getNickname().toLowerCase().equals(nickname.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}
	
	public String[] getScripts()
	{
		String[] scripts = new String[Game.N_TEAMS];
		for(int i = 0; i < Game.N_TEAMS; i++)
		{
			if(selections[i] < 0) return null;
			scripts[i] = this.scripts.get(selections[i]);
		}
		return scripts;
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
		catch (IOException e)
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
			
			io.writeByte(Server.PASSWORD);
			io.writeString(password);
			io.newLine();
			io.writeByte(Server.NICKNAME);
			io.writeString(nickname);
			io.newLine();
			io.flush();
			
			byte b = io.readByte();
			if(b == Server.BAD_PASSWORD)
			{
				ChatLog.logError("Incorrect Password");
				throw new IOException();
			}
			if(b == Server.BAD_NICKNAME)
			{
				ChatLog.logError("That nickname is invalid, or is already in use.");
				throw new IOException();
			}
			if(b != Server.ACCEPTED)
			{
				throw new IOException();
			}
		}
		catch(IOException ex)
		{
			ChatLog.logError("Unable to connect to server");
			return;
		}
		
		ChatLog.logInfo("Connected to server.");
		connected = true;
		onClientConnect();
		
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
						byte b = io.readByte();
						if(b < 0x10)
						{
							String message = io.readLine();
							onServerSentMessage(b, message);
						}
						else if(b == Server.FRAME)
						{
							int index = unsignByte(io.readByte());
							int nObjects = unsignByte(io.readByte());
							int nBytes = nObjects * GameObject.BYTE_SIZE;
							
							byte[] bytes = io.readBytes(nBytes);
							
							Frame frame = new Frame(index, bytes);
							onServerSentFrame(frame);
						}
						else if(b == Server.ITEM_ADD)
						{
							String name = io.readLine();
							onServerSentItem(name);
						}
						else if(b == Server.ITEM_REMOVE)
						{
							int index = Server.unsignByte(io.readByte());
							onServerRemovedItem(index);
						}
						else if(b == Server.ITEM_SELECT)
						{
							int index = Server.unsignByte(io.readByte());
							int team = Server.unsignByte(io.readByte());
							onServerSentSelection(index, team);
						}
						else
						{
							onServerSentByte(b);
						}
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
	
	public void sendMessageToServer(byte type, String message)
	{
		if(message.length() > MAX_MESSAGE_LENGTH)
		{
			ChatLog.logError("Message exceeded max message size.");
			return;
		}
		try
		{
			io.writeByte(type);
			io.writeString(message);
			io.newLine();
			io.flush();
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach server.");
			disconnectFromServer();
		}
	}
	
	public void sendItemRemovalToServer(int item)
	{
		try
		{
			io.writeByte(Server.ITEM_REMOVE);
			io.writeByte(Server.signByte(item));
			io.flush();
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach server.");
			disconnectFromServer();
		}
	}
	
	public void sendItemSelectionToServer(int index, int team)
	{
		try
		{
			io.writeByte(Server.ITEM_SELECT);
			io.writeByte(Server.signByte(index));
			io.writeByte(Server.signByte(team));
			io.flush();
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach server.");
			disconnectFromServer();
		}
	}
	
	public void sendByteToServer(byte b)
	{
		try
		{
			io.writeByte(b);
			io.flush();
		}
		catch(Exception ex)
		{
			ChatLog.logError("Unable to reach server.");
			disconnectFromServer();
		}
	}
	
	private void onClientConnect()
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onClientConnect();
		}
	}
	
	private void onClientDisconnect()
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onClientDisconnect();
		}
	}
	
	private void onServerSentMessage(byte type, String message)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentMessage(type, message);
		}
	}
	
	private void onServerSentFrame(Frame frame)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentFrame(frame);
		}
	}
	
	private void onServerSentItem(String name)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentItem(name);
		}
	}
	
	private void onServerRemovedItem(int index)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerRemovedItem(index);
		}
	}
	
	private void onServerSentSelection(int index, int team)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentSelection(index, team);
		}
	}
	
	private void onServerSentByte(byte b)
	{
		for(ClientListener cl : clientListeners)
		{
			cl.onServerSentByte(b);
		}
	}
	
	public void addServerListener(ServerListener listener)
	{
		serverListeners.add(listener);
	}
	
	public void disconnectFromServer()
	{
		if(connected) ChatLog.logInfo("Disconnected from server.");
		connected = false;
		onClientDisconnect();
		
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
