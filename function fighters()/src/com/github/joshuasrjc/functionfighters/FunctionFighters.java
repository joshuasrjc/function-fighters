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

import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.network.Client;
import com.github.joshuasrjc.functionfighters.network.ClientListener;
import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Server;
import com.github.joshuasrjc.functionfighters.network.ServerListener;
import com.github.joshuasrjc.functionfighters.ui.ChatLog;
import com.github.joshuasrjc.functionfighters.ui.FFMenuBar;
import com.github.joshuasrjc.functionfighters.ui.GameViewer;
import com.github.joshuasrjc.functionfighters.ui.Sprites;

public class FunctionFighters implements ClientListener, ServerListener, ActionListener, ListSelectionListener
{
	public static final int DEFAULT_PORT = 7071;
	
	public static void main(String[] args)
	{
		Sprites.loadSprites();
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
		
		server.addServerListener(this);
		server.addClientListener(this);
		
		createUI();
		
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
		chatLog.CHAT_FIELD.setEnabled(false);
		
		gameViewer = new GameViewer(this);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatLog, gameViewer);
		frame.add(splitPane, BorderLayout.CENTER);
		
		frame.setVisible(true);
		
		fileChooser = new JFileChooser();
		fileChooser.addActionListener(this);
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
	public void onClientSentMessage(Client client, String message)
	{
		
	}
	
	@Override
	public void onClientSentByte(Client client, byte b)
	{
		if(b == Server.GAME_START)
		{
			if(!game.isRunning())
			{
				if(game.start()) server.sendByteToAllClients(Server.GAME_START);
			}
			else if(game.isPaused())
			{
				game.resume();
				server.sendByteToAllClients(Server.GAME_START);
			}
			else
			{
				client.sendMessage(Server.ERROR, "A game is already running.");
			}
		}
		else if(b == Server.GAME_PAUSE)
		{
			if(game.isRunning())
			{
				if(!game.isPaused())
				{
					game.pause();
					server.sendByteToAllClients(Server.GAME_PAUSE);
				}
				else
				{
					client.sendMessage(Server.ERROR, "The game is already paused.");
				}
			}
			else
			{
				client.sendMessage(Server.ERROR, "No game is running.");
			}
		}
		else if(b == Server.GAME_STOP)
		{
			if(game.isRunning())
			{
				game.stop();
				System.out.println("Game stop.");
				server.sendByteToAllClients(Server.GAME_STOP);
			}
			else
			{
				client.sendMessage(Server.ERROR, "No game is running.");
			}
		}
	}
	
	@Override
	public void onClientConnect()
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
	public void onClientDisconnect()
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
	public void onServerSentMessage(byte type, String message)
	{
		switch(type)
		{
		case Server.INFO: ChatLog.logInfo(message); break;
		case Server.CHAT: ChatLog.logChat(message); break;
		case Server.ERROR: ChatLog.logError(message); break;
		case Server.CODE: ChatLog.logCode(message); break;
		default: ChatLog.logInfo(message); break;
		}
	}
	
	@Override
	public void onServerSentItem(String name)
	{
		gameViewer.addItem(name);
	}
	
	@Override
	public void onServerRemovedItem(int index)
	{
		
	}
	
	@Override
	public void onServerSentSelection(int index, int team)
	{
		gameViewer.selectItem(index, team);
	}
	
	@Override
	public void onServerSentFrame(Frame frame)
	{
		gameViewer.addFrame(frame);
	}
	
	@Override
	public void onServerSentByte(byte b)
	{
		if(b == Server.GAME_START)
		{
			gameViewer.setSelectionMode(false);
			menuBar.START.setEnabled(false);
			menuBar.PAUSE.setEnabled(true);
			menuBar.STOP.setEnabled(true);
		}
		else if(b == Server.GAME_PAUSE)
		{
			gameViewer.setSelectionMode(false);
			menuBar.START.setEnabled(true);
			menuBar.PAUSE.setEnabled(false);
			menuBar.STOP.setEnabled(true);
		}
		else if(b == Server.GAME_STOP)
		{
			gameViewer.setSelectionMode(true);
			menuBar.START.setEnabled(true);
			menuBar.PAUSE.setEnabled(false);
			menuBar.STOP.setEnabled(false);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent ev)
	{
		if(!ev.getValueIsAdjusting())
		{
			int index = ev.getFirstIndex();
			int team = Integer.parseInt(((Component)ev.getSource()).getName());
			server.sendItemSelectionToServer(index, team);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev)
	{
		Object src = ev.getSource();

		if(src == menuBar.LOAD)
		{
			fileChooser.showOpenDialog(frame);
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
			server.sendByteToServer(Server.GAME_START);
		}
		else if(src == menuBar.PAUSE)
		{
			server.sendByteToServer(Server.GAME_PAUSE);
		}
		else if(src == menuBar.STOP)
		{
			server.sendByteToServer(Server.GAME_STOP);
		}
		else if(src == chatLog.CHAT_FIELD)
		{
			chat();
		}
		else if(src == fileChooser)
		{
			load();
		}
	}
	
	public void load()
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
		if(file.length() > Server.MAX_MESSAGE_LENGTH)
		{
			ChatLog.logError("That file exceeds the maximum size.");
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
				text += scanner.nextLine() + ' ';
			}
			//text += '\n';
			server.sendMessageToServer(Server.SCRIPT, name + '\n' + text);
		}
		catch(IOException ex)
		{
			ChatLog.logError("Error reading file.");
		}
	}
	
	public void host()
	{
		JTextField nicknameField = new JTextField();
		JTextField portField = new JFormattedTextField("" + DEFAULT_PORT);
		JPasswordField passwordField = new JPasswordField();
		Object[] fields = {
				"Nickname: ", nicknameField,
				"Port: ", portField,
				"Password: ", passwordField
		};
		
		nicknameField.addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent ev)
			{
				nicknameField.requestFocusInWindow();
			}

			@Override public void ancestorMoved(AncestorEvent arg0) {}
			@Override public void ancestorRemoved(AncestorEvent arg0){}
		});
		
		int option = JOptionPane.showConfirmDialog(frame, fields, "Host a Server", JOptionPane.OK_CANCEL_OPTION);
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
		JTextField nicknameField = new JTextField();
		JTextField addressField = new JTextField();
		JTextField portField = new JTextField("" + DEFAULT_PORT);
		JPasswordField passwordField = new JPasswordField();
		
		Object[] fields = {
			"Nickname: ", nicknameField,
			"Server Address: ", addressField,
			"Port: ", portField,
			"Password: ", passwordField
		};
		
		nicknameField.addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent ev)
			{
				nicknameField.requestFocusInWindow();
			}

			@Override public void ancestorMoved(AncestorEvent arg0) {}
			@Override public void ancestorRemoved(AncestorEvent arg0){}
		});
		
		int option = JOptionPane.showConfirmDialog(frame, fields, "Connect to a Server", JOptionPane.OK_CANCEL_OPTION);
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
		server.sendMessageToServer(Server.CHAT, message);
	}
}
