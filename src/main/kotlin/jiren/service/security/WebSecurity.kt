package jiren.service.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler
import java.security.SecureRandom
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
class WebSecurity(
    private val dataSource: DataSource,
    private val credentials: Credentials
) : WebSecurityConfigurerAdapter() {
    // TODO Implement JWT Auth
    @Override
    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth!!.jdbcAuthentication().dataSource(dataSource)
            .passwordEncoder(BCryptPasswordEncoder(15, SecureRandom(credentials.authSecret.toByteArray())))
            .usersByUsernameQuery("select username,password,enabled from users where username=?")
            .authoritiesByUsernameQuery("select u.username, g.name from users u join role g on g.id = u.role_id where u.username = ?")
    }

    @Override
    override fun configure(http: HttpSecurity?) {
        http!!.csrf()?.disable()!!.requestCache().requestCache(CustomRequestCache()).and().formLogin()
            .loginPage("/login").permitAll().loginProcessingUrl("/login").successForwardUrl("/home").and().logout()
            .logoutSuccessUrl("/home").and().authorizeRequests().antMatchers("/", "/home").permitAll()
            .requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll().and().authorizeRequests()
            .antMatchers(
                "/VAADIN/**",
                "/vaadinServlet/**",
                "/vaadinServlet/UIDL/**",
                "/vaadinServlet/HEARTBEAT/**",
                "/manifest.webmanifest",
                "/sw.js",
                "/offline.html",
                "/icons/**",
                "/images/**",
                "/styles/**",
                "/img/**"
            ).permitAll().mvcMatchers("/admin/**").hasAuthority("ADMIN").mvcMatchers("/team/**").hasAuthority("TEAM")
            .anyRequest().authenticated()
    }

    @Bean
    fun roleHierarchy(): RoleHierarchy? {
        val roleHierarchy = RoleHierarchyImpl()
        val hierarchy = "ADMIN > TEAM \n TEAM > USER"
        roleHierarchy.setHierarchy(hierarchy)
        return roleHierarchy
    }

    @Bean
    fun webSecurityExpressionHandler(roleHierarchy: RoleHierarchy?): DefaultWebSecurityExpressionHandler? {
        val expressionHandler = DefaultWebSecurityExpressionHandler()
        expressionHandler.setRoleHierarchy(roleHierarchy)
        return expressionHandler
    }

}