package com.mup.pop3;

import java.io.InputStream;
import java.util.SortedMap;

public interface Mailbox {
	public boolean open(String user, String password);

	public void close();

	public void refresh();

	public boolean updateTransaction();

	public SortedMap<Integer, MailItem> getMbox();

	public int getCount();

	public long getSize();

	public MailItem getMail(int no);

	public InputStream getMailInputStream(int no) throws Exception;

	public int deleteMail(int no);
}
