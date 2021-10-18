package com.hardcoded;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This code converts unicode emoji into the discord equivalent shortcodes.
 * 
 * @date 2021-10-18
 * @author HardCoded <https://github.com/Kariaro>
 * 
 * @see https://unicode.org/Public/emoji/14.0/emoji-sequences.txt
 */
public class EmojiParser {
	private static final int PRESENTATION_SELECTOR = 0x0000FE0F;
	//private static final int ZERO_WITH_JOINER      = 0x0000200D;
	
	private static final String FILE = "/debug/discord.json";
	private static final String FILE_CLEAN = "/discord_clean.json";
	private static final EmojiMap emojiMap;
	
	public static final void init() {
		
	}
	
	static {
		EmojiMap map = null;
		
		try(InputStream stream = EmojiParser.class.getResourceAsStream(FILE_CLEAN)) {
			map = new GsonBuilder().create().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), EmojiMap.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		if(map == null) {
			try(InputStream stream = EmojiParser.class.getResourceAsStream(FILE)) {
				Type mapType = new TypeToken<Map<String, List<DiscordEmoji>>>() {}.getType();
				Map<String, List<DiscordEmoji>> tmp = new GsonBuilder().create()
					.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), mapType);
				
				map = new EmojiMap();
				for(List<DiscordEmoji> tmpList : tmp.values()) {
					for(DiscordEmoji discordEmoji : tmpList) {
						map.add(discordEmoji);
						
						if(discordEmoji.diversityChildren != null) {
							for(DiscordEmoji emoji : discordEmoji.diversityChildren) {
								map.add(emoji);
							}
						}
					}
				}
				
				FileWriter writer = new FileWriter(new File("src/main/resources/" + FILE_CLEAN));
				new GsonBuilder().create().toJson(map, writer);
				writer.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		emojiMap = map;
	}
	
	/**
	 * Returns a text with all unicode emoji replaced with Discord shortcodes.
	 * @param text the text that should be modified
	 */
	public static String parse(String text) {
		return emojiMap.parse(text.codePoints().toArray());
	}
	
	private static class DiscordEmoji {
		public List<String> names;
		public String surrogates;
		public DiscordEmoji[] diversityChildren;
		
		// Unused fields that exist within the json file
		
		// public List<String> diversity;
		// public double unicodeVersion;
		// public boolean hasDiversity;
		// public boolean hasDiversityParent;
		// public boolean hasMultiDiversity;
		// public boolean hasMultiDiversityParent;
	}
	
	private static class Emoji {
		public final int[] pattern;
		public final String alias;
		
		public Emoji(DiscordEmoji discordEmoji) {
			this.pattern = discordEmoji.surrogates.codePoints().filter(i -> {
				// Some base modifiers are followed by a presentation selector.
				// It is easier to compare emoji if the presentation selector is removed.
				return i != PRESENTATION_SELECTOR;
			}).toArray();
			this.alias = ":%s:".formatted(discordEmoji.names.get(0));
		}
		
		/**
		 * Check if this emoji fully matches the provided input array
		 * and returns how many characters that was matched.
		 * 
		 * @param input the input array
		 * @param index the index of the array
		 * @return how many characters that was matched
		 */
		public int equalsInput(int[] input, int index) {
			final int patternLen = pattern.length;
			final int inputLen = input.length;
			
			for(int i = 0, count = 0, idx = index; i < patternLen; i++, idx++) {
				int value = pattern[i];
				int read = input[idx];
				
				if(read == PRESENTATION_SELECTOR) {
					// If the read character represents the presentation selector
					// we skip it and check if we have more bytes in the input.
					if(idx + 1 >= inputLen) {
						// If we have no more bytes to read we know
						// that we didn't match the pattern.
						return 0;
					}
					
					// Increment idx and read the next character.
					read = input[++idx];
					count++;
				}
				
				if(value == read) {
					// If the read value is equal to the current value
					// we increment the count of matched characters.
					count++;
					
					if(i + 1 < patternLen) {
						// If we have more characters to match but have no more input we return zero.
						if(idx + 1 >= inputLen) return 0;
						
						// If we still have more characters to match we continue.
						continue;
					}
					
					// There are no more characters to match we return the count.
					return count;
				}
				
				// We didn't match any value so we return zero.
				return 0;
			}
			
			// Not reachable.
			return 0;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(this.pattern);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Emoji)) return false;
			return Arrays.equals(this.pattern, ((Emoji)obj).pattern);
		}
	}
	
	private static class EmojiMap {
		private final Map<Integer, Set<Emoji>> map = new HashMap<>();
		
		private void add(DiscordEmoji discordEmoji) {
			Emoji emoji = new Emoji(discordEmoji);

			// Java defines that if the added element matches any other element
			// by using the operation Objects.equals(emoji, element[n]) then the
			// set remains unchanged and the emoji is not added.
			//
			// This is the desired function because emoji with equal patterns
			// are evaluated as the same emoji in this code.
			map.computeIfAbsent(emoji.pattern[0], (i) -> new HashSet<>())
				.add(emoji);
		}
		
		public String parse(int[] inputCodePoints) {
			final int inputLen = inputCodePoints.length;
			StringBuilder sb = new StringBuilder();
			int index = 0;
			
			while(index < inputLen) {
				int firstCodePoint = inputCodePoints[index];
				
				Emoji emoji = null;
				int count = 0;
				{
					Set<Emoji> set = map.get(firstCodePoint);
					if(set != null) {
						for(Emoji e : set) {
							int e_count = e.equalsInput(inputCodePoints, index);
							if(e_count > count) {
								count = e_count;
								emoji = e;
							}
						}
					}
				}
				
				if(emoji == null) {
					index++;
					
					// If the value contains information in the higher 16 bits we discard it.
					if((firstCodePoint >>> 16) != 0) {
						continue;
					} else if(firstCodePoint == PRESENTATION_SELECTOR) {
						// If the value is a presentation selector we discard it.
						continue;
					}
					
					sb.append((char)firstCodePoint);
				} else {
					index += count;
					sb.append(emoji.alias);
				}
			}
			
			return sb.toString();
		}
	}
}
