package com.github.joshuasrjc.functionfighters.network;

public interface ServerListener
{
	public void onServerStart();
	public void onServerStop();
	public void onClientSentMessage(Client client, String message);
	public void onClientSentByte(Client client, byte b);
}
