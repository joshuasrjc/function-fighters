package com.github.joshuasrjc.functionfighters.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import com.github.joshuasrjc.functionfighters.network.ClientListener;
import com.github.joshuasrjc.functionfighters.network.Packet;

public class ChatLog extends JPanel implements ClientListener
{
	public static final Dimension MIN_SIZE = new Dimension(64,64);
	public static final Dimension DEFAULT_SIZE = new Dimension(400, 600);
	public static final Font CHAT_FIELD_FONT = new Font("Courier New", Font.PLAIN, 16);
	public static final Color TEXT_PANE_COLOR = new Color(.1f, .1f, .1f);
	public static final Color CHAT_FIELD_COLOR = Color.BLACK;
	public static final Color FONT_COLOR = Color.WHITE;
	
	
	private static ArrayList<ChatLog> logs = new ArrayList<ChatLog>();
	
	private static final StyleContext STYLE_CONTEXT = new StyleContext();
	private static final Style INFO_STYLE = STYLE_CONTEXT.addStyle("Info", null);
	private static final Style ERROR_STYLE = STYLE_CONTEXT.addStyle("Error", null);
	private static final Style CHAT_STYLE = STYLE_CONTEXT.addStyle("Chat", null);
	private static final Style CODE_STYLE = STYLE_CONTEXT.addStyle("Code", null);
	
	public static void initStyles()
	{
		INFO_STYLE.addAttribute(StyleConstants.Foreground, Color.CYAN);
		INFO_STYLE.addAttribute(StyleConstants.FontSize, 14);
		INFO_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");

		ERROR_STYLE.addAttribute(StyleConstants.Foreground, new Color(1f, .5f, .25f));
		ERROR_STYLE.addAttribute(StyleConstants.FontSize, 14);
		ERROR_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		
		CHAT_STYLE.addAttribute(StyleConstants.Foreground, Color.WHITE);
		CHAT_STYLE.addAttribute(StyleConstants.FontSize, 14);
		CHAT_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		
		CODE_STYLE.addAttribute(StyleConstants.Foreground, Color.GRAY);
		CODE_STYLE.addAttribute(StyleConstants.FontSize, 14);
		CODE_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		CODE_STYLE.addAttribute(StyleConstants.Bold, true);
	}
	
	public static void logError(String message)
	{
		System.err.println(message);
		for(ChatLog log : logs)
		{
			log.log(message, ERROR_STYLE);
		}
	}
	
	public static void logInfo(String message)
	{
		System.out.println(message);
		for(ChatLog log : logs)
		{
			log.log(message, INFO_STYLE);
		}
	}
	
	public static void logChat(String message)
	{
		System.out.println(message);
		for(ChatLog log : logs)
		{
			log.log(message, CHAT_STYLE);
		}
	}
	
	public static void logCode(String message)
	{
		System.out.println(message);
		for(ChatLog log : logs)
		{
			log.log(message, CODE_STYLE);
		}
	}
	
	public final JTextField CHAT_FIELD = new JTextField();
	
	private JScrollPane scrollPane;
	private JTextPane textPane;
	private DefaultStyledDocument doc = new DefaultStyledDocument(STYLE_CONTEXT);
	
	ChatLog()
	{
		createUI();
	}
	
	public ChatLog(ActionListener listener)
	{
		createUI();
		
		CHAT_FIELD.setFont(CHAT_FIELD_FONT);
		CHAT_FIELD.setBackground(CHAT_FIELD_COLOR);
		CHAT_FIELD.setForeground(FONT_COLOR);
		CHAT_FIELD.setCaretColor(FONT_COLOR);
		CHAT_FIELD.addActionListener(listener);
		this.add(CHAT_FIELD, BorderLayout.SOUTH);
	}
	
	private void createUI()
	{
		logs.add(this);

		this.setLayout(new BorderLayout());
		this.setMinimumSize(MIN_SIZE);
		this.setPreferredSize(DEFAULT_SIZE);
		
		textPane = new JTextPane(doc);
		textPane.setEditable(false);
		textPane.setBackground(TEXT_PANE_COLOR);
		
		scrollPane = new JScrollPane(textPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	private void log(String message, Style style)
	{
		try
		{
			doc.insertString(doc.getLength(), message + '\n', style);
		}
		catch(Exception e)
		{
			
		}
		
		scrollPane.revalidate();
		JScrollBar sb = scrollPane.getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}

	@Override
	public void onConnectToServer()
	{
		logInfo("Connected to server.");
	}

	@Override
	public void onDisconnectFromServer()
	{
		logInfo("Disconnected from server.");
	}

	@Override
	public void onServerSentPacket(Packet packet)
	{
		if(packet.isMessage())
		{
			int type = packet.type;
			String message = packet.getMessage();
			switch(type)
			{
			case Packet.INFO: log(message, INFO_STYLE); break;
			case Packet.ERROR: log(message, ERROR_STYLE); break;
			case Packet.CHAT: log(message, CHAT_STYLE); break;
			case Packet.CODE: log(message, CODE_STYLE); break;
			}
		}
	}
}
