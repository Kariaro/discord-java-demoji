# discord-java-demoji

This library supports Emoji 13.1 and converts all unicode emojis into Discord shortcodes.

```java
String result = EmojiParser.parse("Hello world! ☺");
System.out.println(result); // "Hello world! :relaxed:"
```

Unit tests can be found in `src/test/java/test`

## Generating the mappings
This library requires a mapping file to correctly convert emoji to shortcodes.
If you want to update the `emoji.json` file to handle more shortcodes you can run the mapping generator `com.hardcoded.generator.EmojiMappingGenerator`.

There is no general solution to generating the mappings for this project and the format discord uses could change in the future.

The internal format of this project will not change.