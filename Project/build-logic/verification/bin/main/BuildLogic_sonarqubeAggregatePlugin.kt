/**
 * Precompiled [build-logic.sonarqube-aggregate.gradle.kts][Build_logic_sonarqube_aggregate_gradle] script plugin.
 *
 * @see Build_logic_sonarqube_aggregate_gradle
 */
public
class BuildLogic_sonarqubeAggregatePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Build_logic_sonarqube_aggregate_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
