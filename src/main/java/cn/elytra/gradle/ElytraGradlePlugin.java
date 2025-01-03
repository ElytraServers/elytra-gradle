package cn.elytra.gradle;

import cn.elytra.gradle.task.GenerateLanguageFilesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;

/**
 * The Elytra Gradle Plugin.
 * <p>
 * This plugin is designed to be a helpful tool for developing legacy Minecraft 1.7.10 Mods, where developers usually put their
 * localization texts in the code, and generate the .lang files by custom Gradle tasks.
 * <p>
 * More functionalities will be added here if someone wanted, and I'm able to implement it.
 *
 * @author Taskeren
 */
public class ElytraGradlePlugin implements Plugin<Project> {

	@Override
	public void apply(Project target) {
		final TaskContainer tasks = target.getTasks();

		tasks.register("generateLanguageFiles", GenerateLanguageFilesTask.class, task -> {
			task.setGroup("elytra");
			task.setDescription("Generates the language files by the comments in source code with special pattern.");

			var sourceSetContainer = target.getExtensions().getByType(SourceSetContainer.class);

			FileCollection allSource = target.files();
			for(SourceSet sourceSet : sourceSetContainer) {
				allSource = allSource.plus(sourceSet.getAllSource());
			}

			task.sourceFiles = allSource;
		});
	}
}
