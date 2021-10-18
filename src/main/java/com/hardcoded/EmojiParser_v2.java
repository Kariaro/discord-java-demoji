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

public class EmojiParser_v2 {
	private static final String FILE = "/debug/discord.json";
	private static final EmojiMap emojiMap;
	
	static {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
		emojiMap = new EmojiMap();
		
		try(InputStream stream = EmojiParser_v2.class.getResourceAsStream(FILE)) {
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
		
		// public double unicodeVersion;
		// public List<String> diversity;
		// public boolean hasDiversity;
		// public boolean hasDiversityParent;
		// public boolean hasMultiDiversity;
		// public boolean hasMultiDiversityParent;
	}
	
	public static class Emoji {
		public final int[] codePoints;
		public final String defaultAlias;
		
		public Emoji(DiscordEmoji discordEmoji) {
			this.codePoints = discordEmoji.surrogates.codePoints().toArray();
			this.defaultAlias = ":%s:".formatted(discordEmoji.names.get(0));
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(codePoints);
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
				int[] emojiCodePoints = emoji.codePoints;
				
				// We do not have enough bytes to encode this emoji
				if(codePoints.length - index - emojiCodePoints.length < 0)
					continue;
				
				if(Arrays.equals(codePoints, index, index + emojiCodePoints.length, emojiCodePoints, 0, emojiCodePoints.length)) {
					if(emojiCodePoints.length > longest) {
						longest = emojiCodePoints.length;
						target = emoji;
					}
				}
			}
			
			return target;
		}
	}
}
