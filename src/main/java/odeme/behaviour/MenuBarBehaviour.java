package odeme.behaviour;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;


public class MenuBarBehaviour {

	private JMenuBar menuBar;

	public MenuBarBehaviour(JFrame frame) {
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
	}

	public void show() {
		// File Menu
		final String[] items_file =  {"Save"       , "Save As"    , "Save as PNG" , null, "Exit"       };
		final int[] keyevents_file = {KeyEvent.VK_S, KeyEvent.VK_A, 0             , 0   , KeyEvent.VK_X};
		final String[] keys_file =   {"control S"  ,"control A"   , null          , null, "control X"  };
		final String[] images_file = {"save_icon"  , "save_icon"  , "png_icon"    , null, "exit_icon"  };

		addMenu("File", KeyEvent.VK_F, items_file, keyevents_file, keys_file, images_file);

	}

	private void addMenu(String name, int key_event, String[] items, int[] keyevents, String[] keys, String[] images) {

		JMenu menu = new JMenu(name);
		menu.setMnemonic(key_event);
		menu.setBorder( new EmptyBorder(10,20,10,20));

		menuBar.add(menu);
	}
}    
