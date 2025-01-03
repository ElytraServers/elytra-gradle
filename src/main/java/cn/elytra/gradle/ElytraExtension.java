package cn.elytra.gradle;

import org.gradle.api.Project;

import javax.inject.Inject;

// Useless Extension for now. Maybe useful later?
public abstract class ElytraExtension {

	public static ElytraExtension get(Project project) {
		return project.getExtensions().getByType(ElytraExtension.class);
	}

	@Inject
	protected abstract Project getProject();

	@Inject
	public ElytraExtension() {
	}

}
