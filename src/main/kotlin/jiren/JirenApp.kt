package jiren

import com.vaadin.flow.component.dependency.NpmPackage
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.PWA
import com.vaadin.flow.shared.communication.PushMode
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Profile
import java.util.*
import javax.annotation.PostConstruct

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class, MongoAutoConfiguration::class])
@EnableCaching
@Push(PushMode.MANUAL)
@NpmPackage(value = "lumo-css-framework", version = "^4.0.10")
@PWA(name = "Portal - Centro de Suporte Sistemas", shortName = "CSS App")
class JirenApp : SpringBootServletInitializer(), AppShellConfigurator {

    @Override
    override fun configure(builder: SpringApplicationBuilder?): SpringApplicationBuilder {
        return builder!!.sources(JirenApp::class.java)
    }

    @PostConstruct
    @Profile("prod")
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<JirenApp>(*args)
        }
    }

}
