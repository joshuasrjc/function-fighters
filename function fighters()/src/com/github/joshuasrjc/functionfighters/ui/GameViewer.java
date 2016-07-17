package com.github.joshuasrjc.functionfighters.ui;

import java.util.*;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.github.joshuasrjc.functionfighters.game.Bullet;
import com.github.joshuasrjc.functionfighters.game.Fighter;
import com.github.joshuasrjc.functionfighters.game.Game;
import com.github.joshuasrjc.functionfighters.game.GameObject;
import com.github.joshuasrjc.functionfighters.game.Vector2;
import com.github.joshuasrjc.functionfighters.network.ClientListener;
import com.github.joshuasrjc.functionfighters.network.Frame;
import com.github.joshuasrjc.functionfighters.network.Packet;
import com.github.joshuasrjc.functionfighters.network.Server;

public class GameViewer extends JPanel implements Runnable, ClientListener, ListSelectionListener
{
	public Random rand = new Random();
	
	public static final Font LIST_FONT = new Font("Courier New", Font.PLAIN, 24);
	public static final Font TEAM_LABEL_FONT = new Font("Courier New", Font.PLAIN, 32);
	public static final Color LIST_COLOR = new Color(.1f, .1f, .1f);
	public static final Color LABEL_COLOR = Color.BLACK;
	public static final Color FONT_COLOR = Color.WHITE;
	
	public static final String TIE_MESSAGE = "Tie Game";
	public static final String WIN_MESSAGE = "Team %d Wins!";
	
	public static final int BUFFER_SIZE = 10;
	private Thread thread;
	private Server server;

	private Queue<Frame> frames = new LinkedList<Frame>();
	private int index = 0;
	private int newestIndex = 0;
	private Frame currentFrame = null;
	private boolean streaming = false;

	private Vector2 view = Vector2.zero();
	private Star[] stars = new Star[200];
	
	private boolean selectionMode = true;
	
	private ArrayList<JPanel> panels = new ArrayList<JPanel>();
	private ArrayList<JList<String>> lists = new ArrayList<JList<String>>();
	private ArrayList<DefaultListModel<String>> listModels = new ArrayList<DefaultListModel<String>>();
	
	private KeyListener keyListener = new KeyListener()
	{
		@Override
		public void keyPressed(KeyEvent ev)
		{
			if(ev.getKeyCode() == KeyEvent.VK_DELETE)
			{
				if(ev.getSource() instanceof JList)
				{
					JList list = (JList)ev.getSource();
					int index = list.getSelectedIndex();
					if(index >= 0)
					{
						server.sendPacketToServer(new Packet(Packet.ITEM_REMOVE, index));
					}
				}
			}
		}

		@Override public void keyReleased(KeyEvent arg0) {}
		@Override public void keyTyped(KeyEvent arg0) {}
	};
	
	public GameViewer(Server server)
	{
		this.setMinimumSize(new Dimension(256,128));
		this.server = server;
		this.setLayout(new GridLayout(0, Game.N_TEAMS));
		
		for(int i = 0; i < Game.N_TEAMS; i++)
		{
			JPanel panel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Team " + (i+1));
			label.setFont(TEAM_LABEL_FONT);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setBackground(LABEL_COLOR);
			label.setOpaque(true);
			label.setForeground(FONT_COLOR);
			panel.add(label, BorderLayout.NORTH);
			
			DefaultListModel<String> listModel = new DefaultListModel<String>();
			listModels.add(listModel);
			JList<String> list = new JList<String>(listModel);
			lists.add(list);
			list.setName("" + i);
			list.addListSelectionListener(this);
			list.setFont(LIST_FONT);
			list.setBackground(LIST_COLOR);
			list.setForeground(FONT_COLOR);
			list.addKeyListener(keyListener);
			JScrollPane scrollPane = new JScrollPane(list);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			panel.add(scrollPane, BorderLayout.CENTER);
			
			panels.add(panel);
			this.add(panel);
		}
		
		this.revalidate();
		this.repaint();
		
		for(int i = 0; i < stars.length; i++)
		{
			stars[i] = new Star();
		}
		
		thread = new Thread(this);
		thread.start();
	}
	
	@Override
	public void onDisconnectFromServer()
	{
		clearItems();
		setSelectionMode(true);
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
			System.out.println("STOP");
			setSelectionMode(true);
			repaint();
		}
		else if(type == Packet.PLAY_SOUND)
		{
			int id = packet.getIndex();
			Assets.playSound(id);
		}
		else if(type == Packet.ITEM_ADD)
		{
			String name = packet.getMessage();
			addItem(name);
		}
		else if(type == Packet.ITEM_REMOVE)
		{
			int index = packet.getIndex();
			removeItem(index);
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
			if(!frames.isEmpty())
			{
				currentFrame = frames.poll();
				this.repaint();
			}
			try{ Thread.sleep(Game.FRAME_MILLIS); }catch(Exception ex){}
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
	
	public void clearItems()
	{
		for(DefaultListModel<String> list : listModels)
		{
			list.clear();
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
			
			g.setColor(LIST_COLOR);
			g.fillRect(0, 0, w, h);
			
			g.translate(w/2 - (int)view.x, h/2 - (int)view.y);
			Graphics2D g2d = (Graphics2D)g;
			g2d.scale((w-16f)/Game.WIDTH, (w-16f)/Game.WIDTH);
			
			g.setColor(Color.BLACK);
			g.fillRect((int)Game.LEFT, (int)Game.TOP, (int)Game.WIDTH, (int)Game.HEIGHT);
			
			g2d.setColor(Color.WHITE);
			
			for(Star star : stars)
			{
				int x = (int)star.x;
				int y = (int)star.y;
				float z = star.z;
				int s = (int)Math.ceil(3f * z * z * z);
				star.update();
				g2d.fillRect(x, y, s, s);
			}
			
			g2d.setStroke(new BasicStroke(2));
			g2d.drawRect((int)(Game.LEFT), (int)(Game.TOP), (int)(Game.RIGHT - Game.LEFT), (int)(Game.BOTTOM - Game.TOP));

			boolean[] teamAlive = new boolean[Game.N_TEAMS];
			
			if(currentFrame != null)
			{
				for(int i = 0; i < currentFrame.objects.length; i++)
				{
					GameObject obj = currentFrame.objects[i];
					obj.draw(g2d);
					if(obj instanceof Fighter)
					{
						teamAlive[((Fighter)obj).team] = true;
					}
				}
				
				g2d.setColor(Color.WHITE);
				g2d.setFont(TEAM_LABEL_FONT);
				FontMetrics fontMetrics = g2d.getFontMetrics(TEAM_LABEL_FONT);
				
				int nTeamsAlive = 0;
				for(boolean b : teamAlive)
				{
					if(b) nTeamsAlive++;
				}
				if(nTeamsAlive == 0)
				{
					String str = TIE_MESSAGE;
					int x = -fontMetrics.stringWidth(str) / 2;
					int y = -fontMetrics.getAscent() / 2;
					g2d.drawString(str, x, y);
				}
				else if(nTeamsAlive == 1)
				{
					for(int i = 0; i < teamAlive.length; i++)
					{
						if(teamAlive[i])
						{
							String str = String.format(WIN_MESSAGE, i+1);
							int x = -fontMetrics.stringWidth(str) / 2;
							int y = -fontMetrics.getAscent() / 2;
							g2d.drawString(str, x, y);
						}
					}
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
	
	private class Star
	{
		float x;
		float y;
		float z = -1f;
		
		long lastNanos;
		
		Star()
		{
			randomPos();
			lastNanos = System.nanoTime();
		}
		
		private void randomPos()
		{
			x = Game.LEFT + Game.WIDTH * rand.nextFloat();
			y = Game.TOP + Game.HEIGHT * rand.nextFloat();
			if(z != -1f)
			{
				int i = (int)(4*z);
				z = i / 4f + 0.25f * rand.nextFloat();
			}
			else
			{
				z = rand.nextFloat();
			}
		}
		
		public void update()
		{
			long nanos = System.nanoTime();
			float deltaTime = (nanos - lastNanos)/1000000000f;
			lastNanos = nanos;
			
			x += deltaTime * 800f * z*z*z / Game.FRAME_MILLIS;
			while(x > Game.RIGHT)
			{
				x -= Game.WIDTH;
				float temp = x;
				randomPos();
				x = temp;
			}
		}
		
		@Override
		public String toString()
		{
			return String.format("Star [ %.2f , %.2f , %.2f ]", x, y, z);
		}
	}
}
