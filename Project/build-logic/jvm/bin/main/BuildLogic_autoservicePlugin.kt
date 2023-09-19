/**
 * Precompiled [build-logic.autoservice.gradle.kts][Build_logic_autoservice_gradle] script plugin.
 *
 * @see Build_logic_autoservice_gradle
 */
public
class BuildLogic_autoservicePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Build_logic_autoservice_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
