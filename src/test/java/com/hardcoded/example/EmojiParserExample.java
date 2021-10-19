package com.hardcoded.example;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.hardcoded.EmojiParser;

import java.awt.GridLayout;

/**
 * Example class for showing how the library can be used in a graphical application.
 * @author HardCoded
 */
public class EmojiParserExample {
	public static void main(String[] args) {
		// Set the look and feel to our system.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		// Create a new window.
		JFrame frame = new JFrame("EmojiParserExample");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setSize(640, 480);
		frame.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));
		
		// Add a left and right textArea.
		final JTextArea textArea_left = new JTextArea();
		textArea_left.setLineWrap(true);
		
		final JTextArea textArea_right = new JTextArea();
		textArea_right.setLineWrap(true);
		
		// Add scroll panes to both text areas
		JScrollPane scrollPane_left = new JScrollPane(textArea_left);
		scrollPane_left.setBorder(new LineBorder(Color.lightGray, 5));
		frame.getContentPane().add(scrollPane_left);
		
		JScrollPane scrollPane_right = new JScrollPane(textArea_right);
		scrollPane_right.setBorder(new LineBorder(Color.lightGray, 5));
		frame.getContentPane().add(scrollPane_right);
		
		// Ensure that the parser has initialized.
		EmojiParser.init();
		
		// Create a deamon thread that will update the right
		// textArea when you modify the left textArea.
		Thread thread = new Thread(() -> {
			try {
				long last_modified = 0;
				boolean modified = false;
				String last_text = "";
				while(true) {
					Thread.sleep(100);
					long now = System.currentTimeMillis();
					
					String curr_text = textArea_left.getText();
					
					if(!last_text.equals(curr_text)) {
						modified = true;
						last_text = curr_text;
						last_modified = now + 200;
					}
					
					if(modified && last_modified < now) {
						modified = false;
						
						long time = System.nanoTime();
						String modified_text = EmojiParser.parse(curr_text);
						time = System.nanoTime() - time;
						
						System.out.printf("Took: %.4f ms\n", time / 1000000.0);
						textArea_right.setText(modified_text);
					}
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		});
		thread.setDaemon(true);
		thread.start();
		
		// Make the window visible.
		frame.setVisible(true);
	}
}
