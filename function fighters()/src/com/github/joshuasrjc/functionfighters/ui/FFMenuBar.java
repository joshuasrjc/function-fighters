package com.github.joshuasrjc.functionfighters.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class FFMenuBar extends JMenuBar
{
	private static final long serialVersionUID = -6570953125278141077L;
	
	public static final Font MENU_FONT = new Font("Courier New", Font.PLAIN, 16);

	private JMenu file;
	public final JMenuItem LOAD = new JMenuItem("Load a Script");
	
	private JMenu network;
	public final JMenuItem HOST = new JMenuItem("Host a Server");
	public final JMenuItem CLOSE = new JMenuItem("Close Server");
	public final JMenuItem CONNECT = new JMenuItem("Connect to a Server");
	public final JMenuItem DISCONNECT = new JMenuItem("Disconnect");
	public final JMenuItem START = new JMenuItem("Start Game");
	public final JMenuItem PAUSE = new JMenuItem("Pause Game");
	public final JMenuItem STOP = new JMenuItem("Stop Game");
	
	private JMenu help;
	public final  JMenuItem GETTING_STARTED = new JMenuItem("Getting Started");
	public final  JMenuItem DOCUMENTATION = new JMenuItem("Documentation");
	public final  JMenuItem ABOUT = new JMenuItem("About");
	
	public FFMenuBar(ActionListener listener)
	{
		this.setMinimumSize(new Dimension(200, 24));
		
		file = new JMenu("File");
		file.setFont(MENU_FONT);
		
		LOAD.setFont(MENU_FONT);
		LOAD.addActionListener(listener);
		LOAD.setEnabled(false);
		file.add(LOAD);
		
		this.add(file);
		
		network = new JMenu("Network");
		network.setFont(MENU_FONT);
		
		HOST.setFont(MENU_FONT);
		HOST.addActionListener(listener);
		network.add(HOST);
		
		CLOSE.setFont(MENU_FONT);
		CLOSE.addActionListener(listener);
		CLOSE.setEnabled(false);
		network.add(CLOSE);
		
		network.addSeparator();

		CONNECT.setFont(MENU_FONT);
		CONNECT.addActionListener(listener);
		network.add(CONNECT);
		
		DISCONNECT.setFont(MENU_FONT);
		DISCONNECT.addActionListener(listener);
		DISCONNECT.setEnabled(false);
		network.add(DISCONNECT);
		
		network.addSeparator();
		
		START.setFont(MENU_FONT);
		START.addActionListener(listener);
		START.setEnabled(false);
		network.add(START);
		
		PAUSE.setFont(MENU_FONT);
		PAUSE.addActionListener(listener);
		PAUSE.setEnabled(false);
		network.add(PAUSE);
		
		STOP.setFont(MENU_FONT);
		STOP.addActionListener(listener);
		STOP.setEnabled(false);
		network.add(STOP);
		
		this.add(network);
		
		help = new JMenu("Help");
		help.setFont(MENU_FONT);

		ABOUT.setFont(MENU_FONT);
		ABOUT.addActionListener(listener);
		help.add(ABOUT);
		
		this.add(help);
	}
}
