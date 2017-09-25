package com.mup.pop3;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZimbraMailBox implements Mailbox {
	private final static Logger log = LoggerFactory.getLogger(ZimbraMailBox.class);

	private ZimbraClient client = null;

	String userId = null;
	// Query, Category Map
	private HashMap<String, String> queryList;
	private long mboxSize;

	private TreeMap<Integer, ZimbraMailItem> mbox;
	private TreeMap<Integer, ZimbraMailItem> deletedMbox;

	SQLiteUIDStore mailStore = null;

	private ZimbraMailBox() {
		client = new ZimbraClient();
		queryList = new HashMap<String, String>();
		mbox = new TreeMap<Integer, ZimbraMailItem>();
		deletedMbox = new TreeMap<Integer, ZimbraMailItem>();

		mailStore = SQLiteUIDStore.getInstance();
	}

	public static ZimbraMailBox create(String serviceUrl) {
		if (serviceUrl == null) {
			log.error("Zimbra service url is missing.");
			return null;
		}
		ZimbraMailBox mbox = new ZimbraMailBox();

		mbox.setServiceUrl(serviceUrl);
		
		return mbox;
	}

	public ZimbraMailBox setFolderList(Map<String, String> folderAndCategory) {
		queryList.clear();
		for (Entry<String, String> elm : folderAndCategory.entrySet()) {
			queryList.put(elm.getKey(), elm.getValue());
		}
		return this;
	}

	public void setServiceUrl(String serviceUrl) {
		client.setServiceUrl(serviceUrl);
	}

	public ZimbraMailBox setDbFile(String dbFileName) {
		mailStore.setDbFileName(dbFileName);
		return this;
	}

	public void setSearchLimit(int searchLimit) {
		client.setSearchLimit(searchLimit);
	}

	public SortedMap<Integer, MailItem> getMbox() {
		SortedMap<Integer, MailItem> map = new TreeMap<Integer, MailItem>(mbox);
		return map;
	}

	public InputStream getMailInputStream(int no) throws Exception {
		return client.getMessage(mbox.get(no).getUid());
	}

	public int getCount() {
		return mbox.size();
	}

	public long getSize() {
		return mboxSize;
	}

	public boolean open(String user, String password) {
		boolean result = client.login(user, password);

		if (result) {
			userId = user;
			mailStore.open(false);
		}

		return result;
	}

	public void close() {
		mailStore.close();
	}

	public ZimbraMailItem getMail(int no) {
		return mbox.get(no);
	}

	public int deleteMail(int no) {
		ZimbraMailItem item = mbox.remove(no);
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

	public Set<Entry<Integer, ZimbraMailItem>> entrySet() {
		return mbox.entrySet();
	}

	public boolean updateTransaction() {
		HashSet<Integer> deleted = new HashSet<Integer>();

		for (Map.Entry<Integer, ZimbraMailItem> elem : deletedMbox.entrySet()) {
			try {
				mailStore.delete(userId, elem.getValue().getUid());
				deleted.add(elem.getKey());
			} catch (SQLException e) {
				log.error("updateTransaction error(UID={}): {}", elem.getValue().getUid(), e.getMessage());
			}
		}
		for (int no : deleted) {
			deletedMbox.remove(no);
		}
		return deletedMbox.isEmpty();
	}

	public void refresh() {
		mboxSize = 0;
		mbox.clear();
		deletedMbox.clear();

		for (Map.Entry<String, String> elem : queryList.entrySet()) {
			String query = elem.getKey();
			String category = elem.getValue();

			List<String> uidList = client.getSearchRequest(query);

			if (uidList == null) {
				log.error("getSearchRequest return null");
				return;
			}
			for (String uid : uidList) {
				try {
					if (!mailStore.isDeleted(userId, uid)) {
						ZimbraMailItem item = new ZimbraMailItem(uid, category);
						mboxSize += item.getSize();
						mbox.put(mbox.size() + 1, item);
					}
				} catch (SQLException e) {
					log.error("SQLite isDeleted error(User={}, UID={}): {}", userId, uid, e.getMessage());
				}
			}
		}
	}
}
