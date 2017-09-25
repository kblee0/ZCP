package com.mup.pop3;

public class ZimbraMailItem extends MailItem {
	public ZimbraMailItem(String id) {
		set(id, null);
	}

	public ZimbraMailItem(String id, String cat) {
		set(id, cat);
	}

	void set(String id, String cat) {
		uid = id;
		category = cat;
		size = 0;

		if (cat != null) {
			// Keywords: {category}
			size += 10 + cat.length() + 2;
		}
	}

	public String toString() {
		String str = "uid:" + uid + ", Size=" + size;
		if (category != null) {
			str += ", category=" + category;
		}
		return str;
	}
}
