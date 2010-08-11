package org.aftff.client;

import java.util.LinkedList;

public class Store extends LinkedList<Ring> {

	public String getAsString() {
		String returnString = "";
		
		for (Ring r : this) {
			returnString = returnString + r.getFullText() + "---";
		}
		return(returnString);
	}

}
