package cn.elytra.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This task will collect the comments with special patterns in the source code files, and generate language files to the output directory.
 * <p>
 * The default localization key pattern is {@code //#tr <key>}, and the localization value pattern is {@code // <lang_code> <translated_text>}. The whitespaces are sensitive, so keep them.
 * The key pattern and the value patterns should be together without an empty line in-between.
 * <p>
 * You also need to add your language codes to the task via {@link #getAllowedLanguageCodes()}, if not, only English ({@code en_US}) is considered as localization text.
 * The language code should be equal to the language filename. For example, if you are adding Simp. Chinese, the language file is {@code zh_CN.lang}, so the language code is {@code zh_CN}.
 * <p>
 * As for existing projects with different patterns, you can set the key pattern via {@link #getKeyPattern()} and the value pattern via {@link #getKeyPattern()}.
 * They are both used to compile into {@link Pattern} for matching. See the default ones in {@link LocalizationTextCollector}.
 * <p>
 * And there is not an option to set which {@link SourceSet} to process, but all files are processed. This should be fixed later, but I don't know how. TwT
 *
 * @author Taskeren
 * @since 1.0
 */
public abstract class GenerateLanguageFilesTask extends DefaultTask {

	/**
	 * @see #getSourceFiles()
	 */
	public FileCollection sourceFiles;

	/**
	 * The default constructor.
	 */
	@Inject
	public GenerateLanguageFilesTask() {
	}

	/**
	 * @return the file collection of source files to read the localization comments.
	 */
	@InputFiles
	public FileCollection getSourceFiles() {
		return sourceFiles;
	}

	/**
	 * Set the source files to read the localization comments.
	 *
	 * @param sourceFiles the file collect
	 */
	public void setSourceFiles(FileCollection sourceFiles) {
		this.sourceFiles = sourceFiles;
	}

	/**
	 * @return the key pattern to match the localization key
	 */
	@Input
	@Optional
	public abstract Property<String> getKeyPattern();

	/**
	 * @return the value pattern to match the localization key
	 */
	@Input
	@Optional
	public abstract Property<String> getValuePattern();

	/**
	 * @return the list of allowed language codes like {@code zh_CN}, {@code en_US}.
	 */
	@Input
	@Optional
	public abstract ListProperty<String> getAllowedLanguageCodes();

	/**
	 * @return the output directory
	 */
	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	/**
	 * @see GenerateLanguageFilesTask
	 */
	@TaskAction
	public void run() {
		var keyPattern = getKeyPattern().getOrNull();
		var valuePattern = getValuePattern().getOrNull();
		var allowedLanguageCodes = getAllowedLanguageCodes().getOrElse(List.of("en_US"));

		var collector = new LocalizationTextCollector();
		if(keyPattern != null) {
			collector.setKeyPattern(keyPattern);
		}
		if(valuePattern != null) {
			collector.setValuePattern(valuePattern);
		}
		collector.setAllowedLanguageCodes(allowedLanguageCodes);

		getLogger().debug("Patterns are prepared, collecting localization data");

		for(File file : sourceFiles) {
			collector.loadSourceFile(file);
		}

		getLogger().debug("Source files are collected, exporting language files");

		for(Map.Entry<String, String> langEntry : collector.buildLanguageFiles().entrySet()) {
			var languageKey = langEntry.getKey();
			var languageContent = langEntry.getValue();

			var languageFileName = languageKey + ".lang";
			try {
				var languageFile = getOutputDirectory().file(languageKey + ".lang").get().getAsFile();
				if(languageFile.exists()) {
					if(!languageFile.delete()) {
						getLogger().warn("Failed to delete existing language file: {}, skipped", languageFile);
						continue;
					}
				}
				if(!languageFile.createNewFile()) {
					getLogger().warn("Failed to create new language file: {}, skipped", languageFile);
					continue;
				}

				Files.writeString(languageFile.toPath(), languageContent, StandardCharsets.UTF_8);
			} catch(IOException e) {
				getLogger().warn("Failed to export language file: {}", languageFileName, e);
			}
		}
	}

	class LocalizationTextCollector {

		@NotNull
		protected Pattern keyPattern;

		@NotNull
		protected Pattern valuePattern;

		@NotNull
		protected List<@NotNull String> allowedLanguageCodes;

		// language code -> translation key -> translation text
		protected final Map<String, Map<String, String>> collector;

		public LocalizationTextCollector() {
			this.collector = new HashMap<>();
			this.keyPattern = Pattern.compile("^//#tr (\\S+)$");
			this.valuePattern = Pattern.compile("^// (\\S+) (.+)$");
			this.allowedLanguageCodes = new ArrayList<>();
		}

		public void setKeyPattern(@NotNull String pattern) {
			this.keyPattern = Pattern.compile(pattern);
		}

		public void setValuePattern(@NotNull String pattern) {
			this.valuePattern = Pattern.compile(pattern);
		}

		public void setAllowedLanguageCodes(@NotNull List<@NotNull String> codes) {
			this.allowedLanguageCodes = codes;
		}

		/**
		 * @param language the language code
		 * @return the localization map for the language
		 */
		public @NotNull Map<String, String> getLocalizationMapFor(@NotNull String language) {
			return collector.computeIfAbsent(language, l -> new LinkedHashMap<>());
		}

		public void add(@NotNull String language, @NotNull String key, @NotNull String value) {
			var languageMap = getLocalizationMapFor(language);
			if(languageMap.containsKey(key)) {
				getLogger().warn("Duplicated key {} in language {}, old \"{}\" new \"{}\"", key, language, languageMap.get(key), value);
			}
			languageMap.put(key, value);
		}

		@Nullable
		public String get(@NotNull String language, @NotNull String key) {
			return getLocalizationMapFor(language).get(key);
		}

		/**
		 * Loads the file and find the patterns and stores the key and values to the collector map.
		 *
		 * @param sourceFile the source file
		 */
		public void loadSourceFile(@NotNull File sourceFile) {
			if(!sourceFile.exists()) {
				getLogger().warn("Source file does not exist: {}", sourceFile);
				return;
			}
			if(!sourceFile.isFile()) {
				getLogger().warn("Source file is not a file: {}", sourceFile);
				return;
			}
			getLogger().debug("Processing source code file: {}", sourceFile);

			try {
				var lines = Files.readAllLines(sourceFile.toPath());

				String key = null;
				for(var line : lines) {
					// trim the whitespaces at the beginning
					line = trimStart(line);

					var keyMatcher = keyPattern.matcher(line);
					if(keyMatcher.find()) {
						key = keyMatcher.group(1);
						getLogger().debug("Found key: {}", key);
						continue;
					}

					var valueMatcher = valuePattern.matcher(line);
					if(valueMatcher.find()) {
						var lang = valueMatcher.group(1);
						var value = valueMatcher.group(2);
						if(!allowedLanguageCodes.contains(lang)) {
							// treat it as a plain comment
							continue;
						}
						if(key == null || key.isEmpty()) {
							getLogger().warn("Invalid value for unknown key in file {}: {}", sourceFile, line);
							continue;
						}
						add(lang, key, value);
						getLogger().debug("Found value for key {}: {}", key, value);
						continue;
					}

					// reset key if both key and value pattern are not matching,
					// which means there is a plain comment.
					key = null;
				}
			} catch(IOException e) {
				getLogger().warn("Failed to read source file: {}", sourceFile, e);
			}
		}

		@NotNull
		public Map<String, String> buildLanguageFiles() {
			var result = new HashMap<String, String>();

			collector.forEach((lang, langData) -> {
				result.put(lang, makeLanguageFile(langData, lang));
			});

			return result;
		}

		private static @NotNull String trimStart(@NotNull String value) {
			for(int i = 0; i < value.length(); i++) {
				if(!Character.isWhitespace(value.charAt(i))) {
					return value.substring(i);
				}
			}
			return "";
		}

		private static @NotNull String makeLanguageFile(@NotNull Map<@NotNull String, @NotNull String> value, @Nullable String languageName) {
			var sb = new StringBuilder("# Auto-generated language file. Don't edit!").append("\n");
			if(languageName != null) {
				sb.append("# The language is ").append(languageName).append("\n");
			}
			var timeText = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SSS").format(new Date());
			sb.append("# Last updated time is ").append(timeText).append("\n");

			value.forEach((k, v) -> {
				sb.append(k).append("=").append(v).append("\n");
			});

			return sb.toString();
		}
	}

}
