package com.nimeshkadecha.drkishan.Helper;

public class Message {
	private String message;
	private double quantity;
	private String quantityType;
	private String date;
	private int k;
	private int interval;

	// Constructor, getters and setters
	public Message(String message, double quantity, String quantityType, String date, int k, int interval) {
		this.message = message;
		this.quantity = quantity;
		this.quantityType = quantityType;
		this.date = date;
		this.k = k;
		this.interval = interval;
	}

	public String getMessage() {
		return message;
	}

	public double getQuantity() {
		return quantity;
	}

	public String getQuantityType() {
		return quantityType;
	}

	public String getDate() {
		return date;
	}

	public int getK() {
		return k;
	}

	public int getInterval() {
		return interval;
	}
}
