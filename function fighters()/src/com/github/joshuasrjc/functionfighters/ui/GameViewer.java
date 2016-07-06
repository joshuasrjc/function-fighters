package com.github.joshuasrjc.functionfighters.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;

import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.game.GameObject;
import com.github.joshuasrjc.functionfighters.game.Vector2;
import com.github.joshuasrjc.functionfighters.network.Frame;

public class GameViewer extends JPanel implements Runnable
{
	public static final Font LIST_FONT = new Font("Courier New", Font.PLAIN, 24);
	public static final Font TEAM_LABEL_FONT = new Font("Courier New", Font.PLAIN, 32);
	
	public static final int BUFFER_SIZE = 10;
	private Thread thread;
	
	private Frame[] frames = new Frame[256];
	private int index = 0;
	private int newestIndex = 0;
	private Frame currentFrame = null;
	private boolean streaming = false;

	private Vector2 view = Vector2.zero();
	
	private boolean selectionMode = true;
	
	private ArrayList<JPanel> panels = new ArrayList<JPanel>();
	private ArrayList<JList<String>> lists = new ArrayList<JList<String>>();
	private ArrayList<DefaultListModel<String>> listModels = new ArrayList<DefaultListModel<String>>();
	
	public GameViewer(ListSelectionListener listener)
	{
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
			list.addListSelectionListener(listener);
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
	public void run()
	{
		while(true)
		{
			if((newestIndex - index + 256) % 256 >= BUFFER_SIZE)
			{
				while((newestIndex - index + 256) % 256 > 0)
				{
					currentFrame = frames[index];
					index++;
					index %= 256;
					repaint();
					try { Thread.sleep(Game.FRAME_MILLIS); } catch(Exception ex) {}
				}
			}
			try { Thread.sleep(Game.FRAME_MILLIS); } catch(Exception ex) {}
		}
	}
	
	public void addFrame(Frame frame)
	{
		if(!selectionMode)
		{
			if(!streaming)
			{
				streaming = true;
				index = frame.index;
			}
			
			newestIndex = frame.index;
			frames[frame.index] = frame;
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
			
			for(int i = 0; i < frames.length; i++)
			{
				frames[i] = null;
			}
		}
		
		this.repaint();
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		int w = this.getWidth();
		int h = this.getHeight();
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		
		if(!selectionMode)
		{
			g.translate(w/2 - (int)view.x, h/2 - (int)view.y);
			
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.BLACK);
			g2d.drawRect((int)(Game.LEFT), (int)(Game.TOP), (int)(Game.RIGHT - Game.LEFT), (int)(Game.BOTTOM - Game.TOP));
			
			if(currentFrame != null)
			{
				for(int i = 0; i < currentFrame.objects.length; i++)
				{
					GameObject obj = currentFrame.objects[i];
					obj.draw(g2d);
				}
				
				this.repaint();
			}
		}
	}
}
