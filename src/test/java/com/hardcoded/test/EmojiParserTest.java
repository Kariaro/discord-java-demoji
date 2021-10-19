package com.hardcoded.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import com.hardcoded.EmojiParser;

/**
 * Test class for checking if all unicode emojis defined by
 * the library is converted into shortcodes.
 * 
 * @author HardCoded
 */
public class EmojiParserTest {
	private static final String SHORTCODE_REGEX = ":[^:]+:";
	private static final String UNICODE_FILE = "/emoji/unicode_emojis";
	
	private String unicodeData;
	
	@Before
	public void setup() {
		try(InputStream stream = EmojiParserTest.class.getResourceAsStream(UNICODE_FILE)) {
			unicodeData = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testParseText() {
		// Parse the unicodeData and replace all unicode emoji with shortcodes.
		String result = EmojiParser.parse(unicodeData);
		
		// Remove all shortcodes from the string.
		result = result.replaceAll(SHORTCODE_REGEX, "");
		
		// Check if the string is empty.
		assertEquals("The string should be empty", 0, result.length());
	}
}
