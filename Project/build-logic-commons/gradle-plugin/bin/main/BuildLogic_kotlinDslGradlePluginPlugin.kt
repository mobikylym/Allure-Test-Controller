/**
 * Precompiled [build-logic.kotlin-dsl-gradle-plugin.gradle.kts][Build_logic_kotlin_dsl_gradle_plugin_gradle] script plugin.
 *
 * @see Build_logic_kotlin_dsl_gradle_plugin_gradle
 */
public
class BuildLogic_kotlinDslGradlePluginPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Build_logic_kotlin_dsl_gradle_plugin_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
