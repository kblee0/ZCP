package com.mup.pop3;

import java.io.File;

import org.apache.commons.codec.binary.Base64;


public class EmlFileMailItem extends MailItem {
	private File emlFile;

	public EmlFileMailItem(String file) {
		set(file, null);
	}

	public EmlFileMailItem(String file, String cat) {
		set(file, cat);
	}

	public File getEmlFile() {
		return emlFile;
	}

	void set(String file, String cat) {
		emlFile = new File(file);
		this.setCategory(cat);
		size = file.length();

		if (cat != null) {
			// Keywords: {category}
			size += 10 + cat.length() + 2;
		}

		String encoded = Base64.encodeBase64String(emlFile.getName().getBytes());
		
		if( encoded.length() < 68 ) {
			this.setUid(encoded);
		}
		else {
			this.setUid(encoded.substring(0, 68));
		}
	}

	public boolean delete() {
		return emlFile.delete();
	}
	public String toString() {
		String str = "File:" + emlFile.getName() + ", Size=" + size;
		if (category != null) {
			str += ", category=" + category;
		}
		return str;
	}
}
