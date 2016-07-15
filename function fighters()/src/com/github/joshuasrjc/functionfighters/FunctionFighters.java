package com.github.joshuasrjc.functionfighters;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.NumberFormat;
import java.util.Scanner;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.github.joshuasrjc.functionfighters.game.Fighter;
import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.network.Client;
import com.github.joshuasrjc.functionfighters.network.ClientListener;
import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.network.ServerListener;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;
import com.github.joshuasrjc.functionfighters.ui.FFMenuBar;
import com.github.joshuasrjc.functionfighters.ui.FileCache;
import com.github.joshuasrjc.functionfighters.ui.GameViewer;
import com.github.joshuasrjc.functionfighters.ui.Assets;

public class FunctionFighters implements ClientListener, ServerListener, ActionListener, ListSelectionListener
{
	public static final String DEFAULT_NICKNAME = "";
	public static final int DEFAULT_PORT = 7070;
	public static final String DEFAULT_ADDRESS = "localhost";
	
	public static void main(String[] args)
	{
		ChatLog.initStyles();
		new FunctionFighters();
	}
	
	private Server server;
	
	private JFrame frame;
	private FFMenuBar menuBar;
	private ChatLog chatLog;
	private Game game;
	private GameViewer gameViewer;
	private JFileChooser fileChooser;
	
	FunctionFighters()
	{
		server = new Server();
		game = new Game(server);
		server.addServerListener(game);
		
		server.addServerListener(this);
		server.addClientListener(this);
		
		createUI();
		Assets.loadAssets();
		FileCache.initCache();
		
		ChatLog.logInfo("Welcome to function fighters()!");
		
		Globals globals = JsePlatform.standardGlobals();
		LuaValue chunk = globals.load("print('hello world');");
		chunk.call();
	}
	
	public void createUI()
	{
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) {}
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		frame = new JFrame("function fighters()");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusable(true);
		Dimension frameSize = new Dimension(2 * screenSize.width / 3, 2 * screenSize.height / 3);
		frame.setSize(frameSize);
		frame.setMinimumSize(new Dimension(400,300));
		frame.setLocation(screenSize.width / 2 - frame.getWidth() / 2, screenSize.height / 2 - 2 * frame.getHeight() / 3);
		
		menuBar = new FFMenuBar(this);
		frame.add(menuBar, BorderLayout.NORTH);
		
		chatLog = new ChatLog(this);
		server.addClientListener(chatLog);
		chatLog.CHAT_FIELD.setEnabled(false);
		
		gameViewer = new GameViewer(server);
		server.addClientListener(gameViewer);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatLog, gameViewer);
		frame.add(splitPane, BorderLayout.CENTER);
		
		frame.setVisible(true);
		
		fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Scripts (.lua)", "lua");
		fileChooser.setFileFilter(filter);
	}
	
	@Override
	public void onServerStart()
	{
		menuBar.HOST.setEnabled(false);
		menuBar.CLOSE.setEnabled(true);
	}
	
	@Override
	public void onServerStop()
	{
		menuBar.HOST.setEnabled(true);
		menuBar.CLOSE.setEnabled(false);
	}
	
	@Override 
	public void onClientConnected(Client client)
	{
		server.sendPacketToAllClients(new Packet(Packet.INFO,
				"Player [" + client.getNickname() + "] has connected to the server."));
	}
	
	@Override 
	public void onClientDisconnected(Client client)
	{
		server.sendPacketToAllClients(new Packet(Packet.INFO,
				"Player [" + client.getNickname() + "] has disconnected from the server."));
	}

	@Override
	public void onClientSentPacket(Client client, Packet packet)
	{
		if(packet.isMessage())
		{
			String message = packet.getMessage();
			String nickname = client.getNickname();
			server.sendPacketToAllClients(new Packet(packet.type, nickname + ": " +  message));
		}
		else if(packet.type == Packet.SCRIPT)
		{
			
		}
	}
	
	@Override
	public void onConnectToServer()
	{
		menuBar.LOAD.setEnabled(true);
		menuBar.HOST.setEnabled(false);
		menuBar.CONNECT.setEnabled(false);
		menuBar.DISCONNECT.setEnabled(true);
		menuBar.START.setEnabled(true);
		chatLog.CHAT_FIELD.setEnabled(true);
		chatLog.CHAT_FIELD.requestFocusInWindow();
	}
	
	@Override
	public void onDisconnectFromServer()
	{
		menuBar.LOAD.setEnabled(false);
		menuBar.HOST.setEnabled(!server.isHosting());
		menuBar.CONNECT.setEnabled(true);
		menuBar.DISCONNECT.setEnabled(false);
		menuBar.START.setEnabled(false);
		menuBar.PAUSE.setEnabled(false);
		menuBar.STOP.setEnabled(false);
		chatLog.CHAT_FIELD.setEnabled(false);
	}

	@Override
	public void onServerSentPacket(Packet packet)
	{
		int type = packet.type;
		if(type == Packet.GAME_START)
		{
			menuBar.START.setEnabled(false);
			menuBar.PAUSE.setEnabled(true);
			menuBar.STOP.setEnabled(true);
		}
		else if(type == Packet.GAME_PAUSE)
		{
			gameViewer.setSelectionMode(false);
			menuBar.START.setEnabled(true);
			menuBar.PAUSE.setEnabled(false);
			menuBar.STOP.setEnabled(true);
		}
		else if(type == Packet.GAME_STOP)
		{
			menuBar.START.setEnabled(true);
			menuBar.PAUSE.setEnabled(false);
			menuBar.STOP.setEnabled(false);
		} 
	}
	
	/*
	@Override
	public void onServerSentFrame(Frame frame)
	{
		gameViewer.addFrame(frame);
	}
	*/

	@Override
	public void valueChanged(ListSelectionEvent ev)
	{
		if(!ev.getValueIsAdjusting())
		{
			int index = ev.getFirstIndex();
			int team = Integer.parseInt(((Component)ev.getSource()).getName());
			server.sendPacketToServer(new Packet(Packet.ITEM_SELECT, index, team));
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev)
	{
		Object src = ev.getSource();

		if(src == menuBar.LOAD)
		{
			load();
		}
		else if(src == menuBar.HOST)
		{
			host();
		}
		else if(src == menuBar.CLOSE)
		{
			server.stopHosting();
		}
		else if(src == menuBar.CONNECT)
		{
			connect();
		}
		else if(src == menuBar.DISCONNECT)
		{
			server.disconnectFromServer();
		}
		else if(src == menuBar.START)
		{
			server.sendPacketToServer(new Packet(Packet.GAME_START));
		}
		else if(src == menuBar.PAUSE)
		{
			server.sendPacketToServer(new Packet(Packet.GAME_PAUSE));
		}
		else if(src == menuBar.STOP)
		{
			server.sendPacketToServer(new Packet(Packet.GAME_STOP));
		}
		else if(src == chatLog.CHAT_FIELD)
		{
			chat();
		}
	}
	
	public void load()
	{
		int option = fileChooser.showOpenDialog(frame);
		if(option == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			
			if(!file.exists() || !file.isFile())
			{
				ChatLog.logError("That file does not exist.");
				return;
			}
			if(!file.canRead())
			{
				ChatLog.logError("That file is not readable.");
				return;
			}
			
			try
			{
				String name = file.getName();
				String text = "";
				FileInputStream in = new FileInputStream(file);
				Scanner scanner = new Scanner(in);
				while(scanner.hasNext())
				{
					text += scanner.nextLine() + '\n';
				}
				scanner.close();
				server.sendPacketToServer(new Packet(Packet.SCRIPT, name + '\n' + text));
			}
			catch(IOException ex)
			{
				ChatLog.logError("Error reading file.");
			}
		}
	}
	
	private JTextField createNicknameField()
	{
		String nickname = FileCache.getString(FileCache.NICKNAME);
		if(nickname == null) nickname = DEFAULT_NICKNAME;
		JTextField field = new JTextField(nickname);
		
		field.addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent ev)
			{
				field.requestFocusInWindow();
			}

			@Override public void ancestorMoved(AncestorEvent arg0) {}
			@Override public void ancestorRemoved(AncestorEvent arg0){}
		});
		
		return field;
	}
	
	private JTextField createAddressField()
	{
		String address = FileCache.getString(FileCache.ADDRESS);
		if(address == null) address = DEFAULT_ADDRESS;
		return new JTextField(address);
	}
	
	private JTextField createPortField()
	{
		String port = FileCache.getString(FileCache.PORT);
		if(port == null) port = "" + DEFAULT_PORT;
		return new JTextField(port);
	}
	
	public void host()
	{
		JTextField nicknameField = createNicknameField();
		JTextField portField = createPortField();
		JPasswordField passwordField = new JPasswordField();
		Object[] fields = {
				"Your Nickname: ", nicknameField,
				"Port: ", portField,
				"Password: ", passwordField
		};
		
		int option = JOptionPane.showConfirmDialog(frame, fields, "Host a Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		
		if(option == JOptionPane.OK_OPTION)
		{
			String nickname = nicknameField.getText();
			String portString = portField.getText();
			String password = new String(passwordField.getPassword());
			
			if(!nickname.equals(""))
			{
				int port;
				try
				{
					port = Integer.parseInt(portString);
				}
				catch(Exception e)
				{
					ChatLog.logError("Invalid port number.");
					return;
				}

				FileCache.cacheString(FileCache.NICKNAME, nickname);
				FileCache.cacheString(FileCache.PORT, "" + port);
				server.startHosting(game, port, password);
				server.connectToServer("localhost", port, nickname, password);
			}
			else
			{
				ChatLog.logError("The nickname field cannot be empty.");
			}
		}
	}
	
	public void connect()
	{
		JTextField nicknameField = createNicknameField();
		JTextField addressField = createAddressField();
		JTextField portField = createPortField();
		JPasswordField passwordField = new JPasswordField();
		
		Object[] fields = {
			"Your Nickname: ", nicknameField,
			"Server Address: ", addressField,
			"Port: ", portField,
			"Password: ", passwordField
		};
		
		int option = JOptionPane.showConfirmDialog(frame, fields, "Connect to a Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if(option == JOptionPane.OK_OPTION)
		{
			String nickname = nicknameField.getText();
			String address = addressField.getText();
			String portString = portField.getText();
			String password = new String(passwordField.getPassword());
			
			if(!nickname.equals(""))
			{
				int port;
				try
				{
					port = Integer.parseInt(portString);
				}
				catch(Exception e)
				{
					ChatLog.logError("Invalid port number.");
					return;
				}
				FileCache.cacheString(FileCache.NICKNAME, nickname);
				FileCache.cacheString(FileCache.ADDRESS, address);
				FileCache.cacheString(FileCache.PORT, "" + port);
				server.connectToServer(address, port, nickname, password);
			}
			else
			{
				ChatLog.logError("The nickname field cannot be empty.");
			}
		}
	}
	
	public void chat()
	{
		String message = chatLog.CHAT_FIELD.getText();
		chatLog.CHAT_FIELD.setText("");
		server.sendPacketToServer(new Packet(Packet.CHAT, message));
	}

}
