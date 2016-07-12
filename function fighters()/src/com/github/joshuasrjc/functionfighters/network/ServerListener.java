package com.github.joshuasrjc.functionfighters.network;

public interface ServerListener
{
	public void onServerStart();
	public void onServerStop();
	public void onClientConnected(Client client);
	public void onClientDisconnected(Client client);
	public void onClientSentPacket(Client client, Packet packet);
	//public void onClientSentMessage(Client client, String message);
	//public void onClientSentByte(Client client, byte b);
}
