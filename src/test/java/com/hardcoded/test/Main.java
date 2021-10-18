package com.hardcoded.test;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.hardcoded.EmojiParser;

import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;

public class Main {
	public static void main(String[] args) {
		new Main();
	}
	
	private JFrame frame;
	private JTextArea textArea_left;
	private JTextArea textArea_right;
	public Main() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		Dimension size = new Dimension(640, 480);
		
		frame = new JFrame("Emoji test");
		frame.setSize(size);
		frame.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));
		
		textArea_left = new JTextArea();
		textArea_left.setBorder(new LineBorder(Color.lightGray, 5));
		textArea_left.setLineWrap(true);
		frame.getContentPane().add(textArea_left);
		
		textArea_right = new JTextArea();
		textArea_right.setBorder(new LineBorder(Color.lightGray, 5));
		textArea_right.setLineWrap(true);
		frame.getContentPane().add(textArea_right);
		
		EmojiParser.init();
		Thread thread = new Thread(() -> {
			try {
				long last_modified = 0;
				boolean modified = false;
				String last_text = "";
				while(true) {
					Thread.sleep(100);
					long now = System.currentTimeMillis();
					
					String curr_text = new String(
						textArea_left.getText().getBytes(StandardCharsets.UTF_8),
						StandardCharsets.UTF_8
					);
					
					if(!last_text.equals(curr_text)) {
						modified = true;
						last_text = curr_text;
						last_modified = now + 500;
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
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		
	}
}
