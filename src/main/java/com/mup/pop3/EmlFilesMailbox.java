package com.mup.pop3;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class EmlFilesMailbox implements Mailbox {
	// Folder, Category Map
	private HashMap<String, String> folderList;
	private long mboxSize;

	private SortedMap<Integer, EmlFileMailItem> mbox;
	private SortedMap<Integer, EmlFileMailItem> deletedMbox;

	public boolean open(String user, String password) {
		return true;
	}

	public void close() {

	}

	private EmlFilesMailbox() {
		folderList = new HashMap<String, String>();
		mbox = new TreeMap<Integer, EmlFileMailItem>();
		deletedMbox = new TreeMap<Integer, EmlFileMailItem>();
	}

	public static EmlFilesMailbox create() {
		return new EmlFilesMailbox();
	}

	public EmlFilesMailbox setFolderList(Map<String, String> folderAndCategory) {
		folderList.clear();
		for (Entry<String, String> elm : folderAndCategory.entrySet()) {
			folderList.put(elm.getKey(), elm.getValue());
		}
		return this;
	}

	public int getCount() {
		return mbox.size();
	}

	public long getSize() {
		return mboxSize;
	}

	public MailItem getMail(int no) {
		return mbox.get(no);
	}

	public InputStream getMailInputStream(int no) throws Exception {
		return new FileInputStream(mbox.get(no).getEmlFile());
	}

	public int deleteMail(int no) {
		EmlFileMailItem item = mbox.remove(no);
		// Success
		if (item != null) {
			deletedMbox.put(no, item);
			return 1;
		}
		// already deleted
		if (deletedMbox.get(no) != null) {
			return 0;
		}
		// not found
		return -1;
	}

	public SortedMap<Integer, MailItem> getMbox() {
		SortedMap<Integer, MailItem> map = new TreeMap<Integer, MailItem>(mbox);
		return map;
	}

	public boolean updateTransaction() {
		HashSet<Integer> deleted = new HashSet<Integer>();
		
		for (Map.Entry<Integer, EmlFileMailItem> elem : deletedMbox.entrySet()) {
			if( elem.getValue().delete() ) {
				deleted.add( elem.getKey());
			}
			else {
			}
		}
		for(int no : deleted) {
			deletedMbox.remove(no);
		}
		return deletedMbox.isEmpty();
	}
	
	public void refresh() {
		mboxSize = 0;
		mbox.clear();
		deletedMbox.clear();
		
		for( Map.Entry<String, String> elem : folderList.entrySet()) {
			String folder = elem.getKey();
			String category = elem.getValue();
			
			File dir= new File(folder);
			
			File[] files = dir.listFiles(new FileFilter() {
						public boolean accept(File file) {
					if (file.isFile() && Pattern.matches(".+\\.(?i)eml$", file.getName())) {
								return true;
							}
							return false;
						}
			});
			
			for (File eml : files) {
				EmlFileMailItem item = new EmlFileMailItem(eml.getAbsolutePath(), category);
				mboxSize += item.getSize();
				mbox.put(mbox.size() + 1, item);
			}

		}
		
	}
}
