package com.mup.pop3;

public class MailItem {
	String uid = null;
	String category = null;
	long size = 0;

	public MailItem() {

	}

	public MailItem(String uid, String category, long size) {
		this.uid = uid;
		this.category = category;
		this.size = size;
	}

	public void set(String uid, String category, long size) {
		this.uid = uid;
		this.category = category;
		this.size = size;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

}
