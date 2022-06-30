package jiren.service.controller

import jiren.data.entity.Log
import jiren.data.entity.User
import jiren.data.enum.Parameters
import jiren.data.repository.log.LogRepository
import jiren.data.repository.log.LogSpecification
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.security.SecurityService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Controller
import java.sql.Timestamp
import java.time.Instant.now
import javax.annotation.PostConstruct

@Controller
class LogController(
    private var logRepository: LogRepository,
    private var userController: UserController,
    private var parameterRepository: ParameterRepository
) {
    private var specification = LogSpecification()
    private var systemUser: User? = null

    @PostConstruct
    private fun init() {
        try {
            systemUser = userController.findByUsername(
                parameterRepository.findByCode(Parameters.SYSTEM_USERNAME.toString())?.value ?: "jiren"
            )
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun search(
        text: String
    ): Page<Log>? {
        return logRepository.findAll(
            Specification.where(
                specification.cmd(text).or(specification.code(text)).or(specification.sql(text))
                    .or(specification.task(text)).or(specification.value(text))
            ), Pageable.ofSize(50)
        )
    }

    fun log(
        logValue: String,
        elapsedTime: Long?,
        task: String?,
        sql: String,
        commandType: String?,
        logCode: String?
    ) {
        val log = Log()
        log.user = userController.findByUsername(SecurityService().authenticatedUser?.username ?: "") ?: systemUser
        log.value = logValue
        log.elapsedTime = elapsedTime
        log.task = task
        log.sqlScript = sql
        log.sqlType = commandType
        log.instant = Timestamp.from(now())
        log.code = logCode ?: "SQLGenericLogger"
        logRepository.save(log)
    }

    fun automationLog(automation: String?, logValue: String, elapsedTime: Long?) {
        val log = Log()
        log.value = logValue
        log.elapsedTime = elapsedTime
        log.user = userController.findByUsername(SecurityService().authenticatedUser?.username ?: "") ?: systemUser
        log.code = "Automation::ERROR"
        log.task = automation
        log.instant = Timestamp.from(now())
        logRepository.save(log)
    }

    fun monitoringLog(monitoring: String?, error: String) {
        val log = Log()
        log.code = "Monitoring::ERROR"
        log.value = error
        log.task = monitoring
        log.instant = Timestamp.from(now())
        log.user = userController.findByUsername(SecurityService().authenticatedUser?.username ?: "") ?: systemUser
        logRepository.save(log)
    }

    fun systemLog(function: String, value: String) {
        val log = Log()
        log.code = "System::$function"
        log.value = value
        log.instant = Timestamp.from(now())
        log.user = systemUser
        logRepository.save(log)
    }
}