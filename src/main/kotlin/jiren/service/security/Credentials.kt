package jiren.service.security

import org.atmosphere.config.service.Singleton
import org.json.JSONObject
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Singleton
@Component
class Credentials(
    private var appSecretManager: AppSecretManager,
    private var env: Environment
) {

    var databaseCredentials = JSONObject()
    var mailerCredentials = JSONObject()
    var jiraCredentials = JSONObject()
    var encryptionKey = ""
    var authSecret = ""

    @PostConstruct
    private fun init() {
        try {
            val secretName = env.getProperty("spring.aws.secretsmanager.name")!!
            val secrets = appSecretManager.retrieve(secretName)
            this.databaseCredentials = JSONObject()
                .put("database-host", secrets?.get("database-host")?.asText())
                .put("database-schema", secrets?.get("database-schema")?.asText())
                .put("database-user", secrets?.get("database-user")?.asText())
                .put("database-password", secrets?.get("database-password")?.asText())
            this.mailerCredentials = JSONObject()
                .put("mailer-user", secrets?.get("mailer-user")?.asText())
                .put("mailer-password", secrets?.get("mailer-password")?.asText())
            this.jiraCredentials = JSONObject()
                .put("jira-user", secrets?.get("jira-user")?.asText())
                .put("jira-token", secrets?.get("jira-token")?.asText())
            this.authSecret = "${secrets?.get("app-auth-secret")?.asText()}"
            this.encryptionKey = "${secrets?.get("app-encryption-key")?.asText()}"
        } catch (_: Exception) {}
    }
}