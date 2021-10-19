# discord-java-demoji
[![Release](https://jitpack.io/v/kariaro/discord-java-demoji.svg)](https://jitpack.io/#kariaro/discord-java-demoji)

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

## Usage
To use this library include this in your `build.gradle`

```
repositories {
    jcenter()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.hardcoded:discord-java-demoji:version'
}
```
