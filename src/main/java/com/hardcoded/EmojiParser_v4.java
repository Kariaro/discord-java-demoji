package com.hardcoded;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// https://unicode.org/Public/emoji/14.0/emoji-sequences.txt
public class EmojiParser_v4 {
	private static final int PRESENTATION_SELECTOR = 0x0000FE0F;
	private static final int ZERO_WITH_JOINER      = 0x0000200D;
	
	private static final String FILE = "/debug/discord.json";
	private static final EmojiMap emojiMap;
	
	public static final void init() {
		
	}
	
	static {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
		emojiMap = new EmojiMap();
		
		try(InputStream stream = EmojiParser_v4.class.getResourceAsStream(FILE)) {
			Type mapType = new TypeToken<Map<String, List<DiscordEmoji>>>() {}.getType();
			Map<String, List<DiscordEmoji>> tmp = gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), mapType);
			
			for(List<DiscordEmoji> tmpList : tmp.values()) {
				for(DiscordEmoji discordEmoji : tmpList) {
					emojiMap.add(discordEmoji);
					
					if(discordEmoji.diversityChildren != null) {
						for(DiscordEmoji emoji : discordEmoji.diversityChildren) {
							emojiMap.add(emoji);
						}
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String parse(String text) {
		StringBuilder sb = new StringBuilder();
		
		int[] codePoints = text.codePoints().toArray();
		int index = 0;
		int len = codePoints.length;
		
		while(index < len) {
			Emoji emoji = emojiMap.get(codePoints, index);
			
			if(emoji == null) {
				int value = codePoints[index++];
				
				// If the value contains information in the higher 16 bits we discard it.
				if((value >>> 16) != 0) {
					continue;
				} else if(value == PRESENTATION_SELECTOR) {
					// If the value is a presentation selector we discard it.
					continue;
				}
				
				sb.append((char)value);
			} else {
				// TODO: This should only be done once.
				int count = emoji.equalsInput(codePoints, index);
				
				sb.append(emoji.defaultAlias);
				System.out.printf("match [%s]\n", emoji.defaultAlias);
				index += count;
			}
		}
		
		return sb.toString();
	}
	
	public static class DiscordEmoji {
		public List<String> names;
		public String surrogates;
		public DiscordEmoji[] diversityChildren;
		public List<String> diversity;
		
		// public double unicodeVersion;
		// public boolean hasDiversity;
		// public boolean hasDiversityParent;
		// public boolean hasMultiDiversity;
		// public boolean hasMultiDiversityParent;
	}
	
	public static class Emoji {
		public final int[] codePoints;
		public final String defaultAlias;
		
		public Emoji(DiscordEmoji discordEmoji) {
			this.codePoints = discordEmoji.surrogates.codePoints().filter(i -> {
				// A unicode emoji is represented by a base modifier.
				// Some base modifiers are followed by the presentation selector
				// and some are followed by a deversity modifier.
				//
				// It is easier to compare emoji if the presentation selector is removed.
				return i != PRESENTATION_SELECTOR;
			}).toArray();
			this.defaultAlias = ":%s:".formatted(discordEmoji.names.get(0));
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(codePoints);
		}
		
		public int equalsInput(int[] input, int index) {
			final int codePointLen = codePoints.length;
			final int inputLen = input.length;
			
			for(int i = 0, count = 0, idx = index; i < codePointLen; i++, idx++) {
				int value = codePoints[i];
				int read = input[idx];
				
				if(read == PRESENTATION_SELECTOR) {
					// If the read character represents the presentation selector
					// we skip it and check if we have more bytes in the input.
					if((i + 1 > codePointLen) || (idx + 1 >= inputLen)) {
						// If we have no more bytes to match or to read we know
						// that we didn't match the codePoint.
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
					
					if(i + 1 < codePointLen) {
						// If we have more characters to match but have no more input we return zero.
						if(idx + 1 >= inputLen) return 0;
						
						// If we still have more characters to match we continue.
						continue;
					}
					
					// There are no more characters to match se we can return.
					return count;
				}
				
				// We didn't match any value so we return zero.
				return 0;
			}
			
			// Not reachable.
			return 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Emoji)) return false;
			return Arrays.equals(this.codePoints, ((Emoji)obj).codePoints);
		}
	}
	
	private static class EmojiMap {
		private final Map<Integer, List<Emoji>> map = new HashMap<>();
		private void add(DiscordEmoji discordEmoji) {
			if(discordEmoji == null) return;
			Emoji emoji = new Emoji(discordEmoji);
			
			int first = emoji.codePoints[0];
			
			List<Emoji> list = map.computeIfAbsent(first, (i) -> new ArrayList<>());
			if(list.contains(emoji)) {
				return;
			}
			
			list.add(emoji);
			System.out.printf("Add emoji first: [0x%08x] [%2d] [%s]\n", first, emoji.codePoints.length, emoji.defaultAlias);
		}
		
		public Emoji get(int[] codePoints, int index) {
			int first = codePoints[index];
			
			List<Emoji> list = map.get(first);
			if(list == null) return null;
			
			Emoji target = null;
			int longest = 0;

			for(Emoji emoji : list) {
				int count = emoji.equalsInput(codePoints, index);
				if(count > longest) {
					longest = count;
					target = emoji;
				}
			}
			
			return target;
		}
	}
}
