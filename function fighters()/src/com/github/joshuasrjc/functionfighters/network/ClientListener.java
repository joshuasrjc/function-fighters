package com.github.joshuasrjc.functionfighters.network;

public interface ClientListener
{
	public void onConnectToServer();
	public void onDisconnectFromServer();
	public void onServerSentPacket(Packet packet);
	//public void onServerSentMessage(byte type, String message);
	//public void onServerSentFrame(Frame frame);
	//public void onServerSentItem(String name);
	//public void onServerRemovedItem(int index);
	//public void onServerSentSelection(int index, int team);
	//public void onServerSentByte(byte b);
}
