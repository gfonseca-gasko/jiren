package jiren.service.job.automation

import jiren.service.mailer.SpringMail
import jiren.data.entity.Automation
import jiren.data.enum.Parameters
import jiren.data.enum.StatusAutomation
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.controller.AutomationController
import jiren.service.controller.LogController
import org.quartz.*
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Component
import java.time.Instant.now
import java.util.*
import javax.annotation.PostConstruct

@Component
@DisallowConcurrentExecution
class AutomationJob(
    private val automationController: AutomationController,
    private val parameterRepository: ParameterRepository,
    private val logController: LogController,
    private val mailer: SpringMail,
) : Job {
    private var repeatInterval: Long = 300000
    private var disable: Int = 1
    private var mailTo: String = ""

    @Override
    override fun execute(context: JobExecutionContext) {
        setParameters()
        if (disable == 0) {
            executeAutomations()
            val trigger = TriggerBuilder.newTrigger()
            trigger.withSchedule(
                simpleSchedule().withIntervalInMilliseconds(repeatInterval).repeatForever()
            ).startAt(Date.from(now().plusMillis(repeatInterval))).withIdentity("automationSchedule")
            context.scheduler.rescheduleJob(context.trigger.key, trigger.build())
        }
    }

    @Bean
    fun automationSchedule(): JobDetail? {
        return JobBuilder.newJob(AutomationJob::class.java).withIdentity("automation").storeDurably(true)
            .withDescription("Agenda de execução de automações").build()
    }

    @Bean
    fun automationTrigger(@Qualifier("automationSchedule") automationSchedule: JobDetail?): SimpleTriggerFactoryBean? {
        val trigger = SimpleTriggerFactoryBean()
        trigger.setJobDetail(automationSchedule!!)
        trigger.setRepeatInterval(repeatInterval)
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY)
        return trigger
    }

    @PostConstruct
    private fun init() {
        setParameters()
    }

    private fun executeAutomations() {
        val automationList: List<Automation>? = automationController.findAutomationToRun()
        if (automationList == null || automationList.isEmpty()) return
        automationList.forEach { it.status = StatusAutomation.RUNNING }
        automationController.saveAll(automationList.toList())
        automationList.forEach { automation ->
            try {
                automationController.execute(automation)
            } catch (e: Exception) {
                sendMail(automation, e.message)
                logController.automationLog(automation.name, e.stackTraceToString(), null)
            }
        }
    }

    private fun setParameters() {
        try {
            mailTo =
                parameterRepository.findByCode(Parameters.JOB_AUTOMATION_FAILURE_EMAIL.toString())?.value.toString()
            disable = parameterRepository.findByCode(Parameters.JOB_AUTOMATION_DISABLE.toString())?.value.let {
                if (it.isNullOrEmpty()) "1".toInt()
                else it.toInt()
            }
            repeatInterval = parameterRepository.findByCode(Parameters.JOB_AUTOMATION_INTERVAL.toString())?.value.let {
                if (it.isNullOrEmpty()) "300000".toLong()
                else it.toLong()
            }
        } catch (e: Exception) {
            logController.automationLog("SET_PARAMETERS", e.stackTraceToString(), null)
        }
    }

    private fun sendMail(obj: Automation, error: String?) {
        val msg = StringBuilder()
        msg.appendLine("Automation ${obj.name}")
        msg.appendLine("Confluence ${obj.documentUrl}")
        msg.appendLine("Error -> $error")
        mailer.send(mailTo, "AUTOMATION ${obj.name} HAS FAILED", msg.toString(), null)
    }

}

