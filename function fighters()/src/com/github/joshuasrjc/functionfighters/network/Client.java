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
					byte b = io.readByte();
					if(b != Server.PASSWORD) throw new IOException();
					String password = io.readLine();
					
					b = io.readByte();
					if(b != Server.NICKNAME) throw new IOException();
					String nickname = io.readLine();
					
					//ChatLog.logInfo("Player [" + nickname + "] attempted to join with password [" + password + "]");
					
					if(!server.matchesPassword(password))
					{
						io.writeByte(Server.BAD_PASSWORD);
						io.flush();
						disconnect();
					}
					if(server.isNicknameTaken(nickname))
					{
						io.writeByte(Server.BAD_NICKNAME);
						io.flush();
						disconnect();
					}
					else
					{
						client.nickname = nickname;
						io.writeByte(Server.ACCEPTED);
						io.flush();
						
						server.sendMessageToAllClients(Server.INFO, "Player [" + nickname + "] has connected.");
						
						while(connected)
						{
							b = io.readByte();
							//System.out.println(b);
							if(b < 0x10)
							{
								String message = io.readLine();
								server.onClientSentMessage(client, message, b);
							}
							else if(b == Server.SCRIPT)
							{
								String name = io.readLine();
								String text = io.readLine();
								server.onClientSentScript(client, name, text);
							}
							else if(b == Server.ITEM_REMOVE)
							{
								int index = Server.unsignByte(io.readByte());
								server.onClientSentItemRemoval(index);
							}
							else if(b == Server.ITEM_SELECT)
							{
								int index = Server.unsignByte(io.readByte());
								int team = Server.unsignByte(io.readByte());
								server.onClientSentItemSelection(index, team);
							}
							else
							{
								server.onClientSentByte(client, b);
							}
						}
					}
				}
				catch (IOException e)
				{
					if(connected) disconnect();
				}
			}
			// </Separate Thread> //
			
		});
		thread.start();
	}
	
	public void sendMessage(byte type, String message)
	{
		try
		{
			io.writeByte(type);
			io.writeString(message);
			io.newLine();
			io.flush();
		}
		catch(Exception e)
		{
			ChatLog.logError("Unable to reach client.");
			disconnect();
		}
	}
	
	public void sendFrame(Frame frame)
	{
		try
		{
			io.writeByte(Server.FRAME);
			io.writeByte(Server.signByte(frame.index));
			io.writeByte(Server.signByte(frame.bytes.length / GameObject.BYTE_SIZE));
			io.writeBytes(frame.bytes);
			io.flush();
		}
		catch(Exception e)
		{
			ChatLog.logError("Unable to reach client.");
			disconnect();
		}
	}
	
	public void sendItemRemoval(int index)
	{
		try
		{
			io.writeByte(Server.ITEM_REMOVE);
			io.writeByte(Server.signByte(index));
			io.flush();
		}
		catch(Exception e)
		{
			ChatLog.logError("Unable to reach client.");
			disconnect();
		}
	}
	
	public void sendItemSelection(int index, int team)
	{
		try
		{
			io.writeByte(Server.ITEM_SELECT);
			io.writeByte(Server.signByte(index));
			io.writeByte(Server.signByte(team));
			io.flush();
		}
		catch(Exception e)
		{
			ChatLog.logError("Unable to reach client.");
			disconnect();
		}
	}
	
	public void sendByte(byte b)
	{
		try
		{
			io.writeByte(b);
			io.flush();
		}
		catch(Exception e)
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
