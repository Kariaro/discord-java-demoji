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

public class EmojiParser_v3 {
	private static final int VARIATION_SELECTOR = 0x0000fe0f;
	private static final String FILE = "/debug/discord.json";
	private static final EmojiMap emojiMap;
	
	static {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
		emojiMap = new EmojiMap();
		
		try(InputStream stream = EmojiParser_v3.class.getResourceAsStream(FILE)) {
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
			Emoji match = emojiMap.get(codePoints, index);
			
			String result;
			if(match == null) {
				result = Character.toString(codePoints[index]);
				System.out.printf("      [0x%08x] [%s]\n", codePoints[index], result);
				index++;
			} else {
				result = match.defaultAlias;
				System.out.printf("match [%s]\n", result);
				index += match.codePoints.length;
			}

			sb.append(result);
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
		public final Set<Integer> deversity;
		
		public Emoji(DiscordEmoji discordEmoji) {
			this.codePoints = discordEmoji.surrogates.codePoints().toArray();
			this.defaultAlias = ":%s:".formatted(discordEmoji.names.get(0));
			
			if(discordEmoji.diversity == null) {
				this.deversity = Set.of(0x0000fe0f);
			} else {
				Set<Integer> copy = new HashSet<>();
				copy.add(0x0000fe0f);
				
				for(String str : discordEmoji.diversity) {
					try {
						copy.add(Integer.parseInt(str, 16));
					} catch(NumberFormatException e) {
						// Bad
					}
				}
				
				this.deversity = Set.copyOf(copy);
			}
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(codePoints);
		}
		
		public int equalsInput(int[] input, int index) {
			final int codePointLen = codePoints.length;
			for(int i = 0, count = 0, idx = index; i < codePointLen; i++, idx++) {
				if(idx >= input.length) return 0;
				
				int value = codePoints[i];
				int read = input[idx];
				
				if(read == value) {
					count ++;
					
					if(i + 1 < codePointLen) {
						continue;
					}

					// There are no more characters so we return the count
					return count;
				} else if(deversity.contains(value)) {
					// If this was a deversity character and the values did not match
					
					if(i + 1 >= codePointLen) {
						// If we have no more values and the codePoint matched the
						// specified pattern then we return the current count
						return count;
					}
					
					// If we have more values and the codePoints did not match we check
					// if we match with the next character, but if there are no more characters we return zero
					if(idx + 1 >= input.length) return 0;
					
					if(value != input[idx + 1]) {
						// If we didn't match we should continue
						return 0;
					}
					
					// We matched the next value and should skip the deversity character.
					count++;
					idx++;
					continue;
				} else {
					return 0;
				}
			}
				
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
