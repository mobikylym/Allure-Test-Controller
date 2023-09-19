/**
 * Precompiled [build-logic.publish-to-central.gradle.kts][Build_logic_publish_to_central_gradle] script plugin.
 *
 * @see Build_logic_publish_to_central_gradle
 */
public
class BuildLogic_publishToCentralPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Build_logic_publish_to_central_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
