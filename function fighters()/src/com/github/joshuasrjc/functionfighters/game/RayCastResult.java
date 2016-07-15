package com.github.joshuasrjc.functionfighters.game;

public class RayCastResult
{
	public GameObject hitObject = null;
	public Vector2 hitPoint = null;
	public boolean inside = false;
	
	public RayCastResult(GameObject obj, Vector2 p, boolean b)
	{
		hitObject = obj;
		hitPoint = p;
		inside = b;
	}
	
	public boolean didHitObject()
	{
		return hitObject != null && hitPoint != null;
	}
}
