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

public class ChatLog extends JPanel
{
	public static final Dimension MIN_SIZE = new Dimension(64,64);
	public static final Dimension DEFAULT_SIZE = new Dimension(400, 600);
	public static final Font CHAT_FIELD_FONT = new Font("Courier New", Font.PLAIN, 16);
	
	private static ArrayList<ChatLog> logs = new ArrayList<ChatLog>();
	
	private static final StyleContext STYLE_CONTEXT = new StyleContext();
	private static final Style ERROR_STYLE = STYLE_CONTEXT.addStyle("Error", null);
	private static final Style INFO_STYLE = STYLE_CONTEXT.addStyle("Info", null);
	private static final Style CHAT_STYLE = STYLE_CONTEXT.addStyle("Chat", null);
	private static final Style CODE_STYLE = STYLE_CONTEXT.addStyle("Code", null);
	
	public static void initStyles()
	{
		ERROR_STYLE.addAttribute(StyleConstants.Foreground, Color.RED);
		ERROR_STYLE.addAttribute(StyleConstants.FontSize, 14);
		ERROR_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		
		INFO_STYLE.addAttribute(StyleConstants.Foreground, Color.BLUE);
		INFO_STYLE.addAttribute(StyleConstants.FontSize, 14);
		INFO_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		
		CHAT_STYLE.addAttribute(StyleConstants.Foreground, Color.BLACK);
		CHAT_STYLE.addAttribute(StyleConstants.FontSize, 14);
		CHAT_STYLE.addAttribute(StyleConstants.FontFamily, "courier new");
		
		CODE_STYLE.addAttribute(StyleConstants.Foreground, new Color(0, 191, 0));
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
}
