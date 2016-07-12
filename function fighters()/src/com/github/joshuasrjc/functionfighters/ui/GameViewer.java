package com.github.joshuasrjc.functionfighters.ui;

import java.util.*;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.game.GameObject;
import com.github.joshuasrjc.functionfighters.game.Vector2;
import com.github.joshuasrjc.functionfighters.network.ClientListener;
import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;

public class GameViewer extends JPanel implements Runnable, ClientListener, ListSelectionListener
{
	public static final Font LIST_FONT = new Font("Courier New", Font.PLAIN, 24);
	public static final Font TEAM_LABEL_FONT = new Font("Courier New", Font.PLAIN, 32);
	
	public static final int BUFFER_SIZE = 10;
	private Thread thread;
	private Server server;

	private Queue<Frame> frames = new LinkedList<Frame>();
	private int index = 0;
	private int newestIndex = 0;
	private Frame currentFrame = null;
	private boolean streaming = false;

	private Vector2 view = Vector2.zero();
	
	private boolean selectionMode = true;
	
	private ArrayList<JPanel> panels = new ArrayList<JPanel>();
	private ArrayList<JList<String>> lists = new ArrayList<JList<String>>();
	private ArrayList<DefaultListModel<String>> listModels = new ArrayList<DefaultListModel<String>>();
	
	public GameViewer(Server server)
	{
		this.server = server;
		this.setLayout(new GridLayout(0, Game.N_TEAMS));
		
		for(int i = 0; i < Game.N_TEAMS; i++)
		{
			JPanel panel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Team " + i);
			label.setFont(TEAM_LABEL_FONT);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			panel.add(label, BorderLayout.NORTH);
			
			DefaultListModel<String> listModel = new DefaultListModel<String>();
			listModels.add(listModel);
			JList<String> list = new JList<String>(listModel);
			lists.add(list);
			list.setName("" + i);
			list.addListSelectionListener(this);
			list.setFont(LIST_FONT);
			JScrollPane scrollPane = new JScrollPane(list);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			panel.add(scrollPane, BorderLayout.CENTER);
			
			panels.add(panel);
			this.add(panel);
		}
		
		this.revalidate();
		this.repaint();
		
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void onServerSentPacket(Packet packet)
	{
		int type = packet.type;
		if(type == Packet.GAME_START)
		{
			setSelectionMode(false);
		}
		else if(type == Packet.GAME_PAUSE)
		{
			setSelectionMode(false);
		}
		else if(type == Packet.GAME_STOP)
		{
			setSelectionMode(true);
		} 
		else if(type == Packet.ITEM_ADD)
		{
			String name = packet.getMessage();
			addItem(name);
		}
		else if(type == Packet.ITEM_SELECT)
		{
			int index = packet.getIndex();
			int team = packet.getTeam();
			selectItem(index, team);
		}
		else if(type == Packet.FRAME)
		{
			Frame frame = new Frame(packet);
			addFrame(frame);
		}
	}
	
	@Override
	public void run()
	{
		while(true)
		{
			while(frames.size() < 2)
			{
				try{ Thread.sleep(Game.FRAME_MILLIS); }catch(Exception ex){}
			}
			while(!frames.isEmpty())
			{
				currentFrame = frames.poll();
				this.repaint();
				try{ Thread.sleep(Game.FRAME_MILLIS); }catch(Exception ex){}
			}
		}
	}
	
	public void addFrame(Frame frame)
	{
		if(!selectionMode)
		{
			frames.add(frame);
		}
	}
	
	public void addItem(String name)
	{
		for(DefaultListModel<String> list : listModels)
		{
			list.addElement(name);
		}
	}
	
	public void removeItem(int index)
	{
		for(DefaultListModel<String> list : listModels)
		{
			list.remove(index);
		}
	}
	
	public void selectItem(int index, int team)
	{
		JList<String> list = lists.get(team);
		list.setSelectedIndex(index);
	}
	
	public void setSelectionMode(boolean selectionMode)
	{
		if(!this.selectionMode && selectionMode)
		{
			for(JPanel panel : panels)
			{
				this.add(panel);
			}
		}
		this.selectionMode = selectionMode;
		
		if(!selectionMode)
		{
			this.removeAll();
			
			streaming = false;
			index = 0;
			newestIndex = 0;
			
			frames.clear();
		}
		
		this.repaint();
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		if(!selectionMode)
		{
			
			int w = this.getWidth();
			int h = this.getHeight();
			
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);
			
			g.translate(w/2 - (int)view.x, h/2 - (int)view.y);
			
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.BLACK);
			g2d.drawRect((int)(Game.LEFT), (int)(Game.TOP), (int)(Game.RIGHT - Game.LEFT), (int)(Game.BOTTOM - Game.TOP));
			g2d.drawLine(0, 0, (int)Game.RIGHT, 0);
			
			if(currentFrame != null)
			{
				for(int i = 0; i < currentFrame.objects.length; i++)
				{
					GameObject obj = currentFrame.objects[i];
					obj.draw(g2d);
				}
				
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent ev)
	{
		if(!ev.getValueIsAdjusting())
		{
			if(ev.getSource() instanceof JList)
			{
				JList list = (JList) ev.getSource();
				int index = list.getSelectedIndex();
				int team = Integer.parseInt(list.getName());
				server.sendPacketToServer(new Packet(Packet.ITEM_SELECT, index, team));
			}
		}
	}

	@Override public void onConnectToServer() {}

	@Override public void onDisconnectFromServer() {}
}
