package com.hardcoded.generator;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.hardcoded.EmojiParser.EmojiMap;

/**
 * This class will generate the shortcode mappings that is used by this project
 * to convert from unicode emoji into the Discord equivanelt shortcodes.
 * 
 * @author HardCoded
 */
public class EmojiMappingGenerator {
	private static final Pattern REGEX_PATTERN = Pattern.compile("\\{\"(people|activity|flags|food|nature|objects|symbols|travel)\":.*\\}");
	private static final String DISCORD_URL = "https://discord.com/assets/d4791e9447ff80f4a808.js";
	private static final String PATH = "src/test/resources/generated/emoji.json";
	
	public static void main(String[] args) {
		String bundleString;
		
		System.out.println("Reading bundle file");
		// Read the file located at the input stream
		try(InputStream stream = new URL(DISCORD_URL).openStream()) {
			bundleString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Matching regex pattern");
		Matcher matcher = REGEX_PATTERN.matcher(bundleString);
		if(!matcher.find()) {
			throw new NullPointerException("Could not read the emoji mappings");
		}
		
		System.out.println("Converting format");
		String emojiMapping = bundleString.substring(matcher.start(), matcher.end());
		
		Type mapType = new TypeToken<Map<String, List<DiscordEmoji>>>() {}.getType();
		JsonReader jsonReader = new JsonReader(new StringReader(emojiMapping));
		
		// Create a gson instance.
		Gson gson = new GsonBuilder().create();
		
		// Read the emoji mappings from the read json file
		Map<String, List<DiscordEmoji>> tmp = gson.fromJson(jsonReader, mapType);
		
		// Convert the json into the mapping format.
		EmojiMap emojiMap = new EmojiMap();
		for(List<DiscordEmoji> tmpList : tmp.values()) {
			for(DiscordEmoji discordEmoji : tmpList) {
				emojiMap.add(discordEmoji.surrogates, discordEmoji.names.get(0));
				
				if(discordEmoji.diversityChildren != null) {
					for(DiscordEmoji emoji : discordEmoji.diversityChildren) {
						emojiMap.add(emoji.surrogates, emoji.names.get(0));
					}
				}
			}
		}
		
		System.out.println("Writing generated file");
		// Write the emoji mappings into our resources.
		try(FileWriter jsonWriter = new FileWriter(new File(PATH))) {
			gson.toJson(emojiMap, jsonWriter);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	static class DiscordEmoji {
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
	
}
