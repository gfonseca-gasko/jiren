package jiren.service.security

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import javax.annotation.PostConstruct
import javax.sql.DataSource

@Profile("prod")
@Configuration
class HikariDBConfig(private val credentials: Credentials) {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    fun appDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @Primary
    @PostConstruct
    fun init(): DataSource? {
        return try {
            val host: String? = credentials.databaseCredentials.getString("database-host")
            val dbname: String? = credentials.databaseCredentials.getString("database-schema")
            val username: String? = credentials.databaseCredentials.getString("database-user")
            val password: String? = credentials.databaseCredentials.getString("database-password")
            appDataSourceProperties().url = "jdbc:mysql://$host:3306/$dbname"
            appDataSourceProperties().username = username
            appDataSourceProperties().password = password
            appDataSourceProperties().initializeDataSourceBuilder()?.build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}