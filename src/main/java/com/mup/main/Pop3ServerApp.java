package com.mup.main;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mup.pop3.Pop3Server;

/**
 * Hello world!
 *
 */
public class Pop3ServerApp 
{
	private final static Logger log = LoggerFactory.getLogger(Pop3ServerApp.class);

	public static void main(String[] args)
    {
		if (Pop3Server.getInstance().start() == false) {
			log.error("POP3 service start failed.");
			System.exit(1);
		}
		else {
			showTrayIcon();
		}
    }

	public static void showTrayIcon() {
		final TrayIcon trayIcon = new TrayIcon(
				Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("tray.gif")),
				"Multi Utilities");

		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trayIcon.displayMessage("POP3 Server", "Started", TrayIcon.MessageType.INFO);
			}
		});

		final PopupMenu popup = new PopupMenu();
		
		// Exit Menu
		MenuItem exitItem = new MenuItem("Exit");
		ActionListener exitListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				SystemTray.getSystemTray().remove(trayIcon);
				System.exit(0);
			}
		};
		exitItem.addActionListener(exitListener);

		// Create menu item
		popup.add(exitItem);

		try {
			trayIcon.setPopupMenu(popup);

			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			log.error("TrayIcon could not be added.");
		}
	}
}
