package jiren.service.job.monitoring

import jiren.service.jira.JiraApi
import jiren.service.rocketchat.ChatAPI
import jiren.service.mailer.SpringMail
import jiren.service.controller.LogController
import jiren.service.controller.MonitoringController
import jiren.data.entity.Monitoring
import jiren.data.enum.Parameters
import jiren.data.enum.StatusMonitoring
import jiren.data.repository.parameter.ParameterRepository
import org.quartz.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.util.*
import javax.annotation.PostConstruct

@Service
class MonitoringJob(
    private val monitoringController: MonitoringController,
    private val parameterRepository: ParameterRepository,
    private val logController: LogController,
    private val mailer: SpringMail,
    private val rocketchat: ChatAPI,
    private val jiraApi: JiraApi
) : Job {

    private var repeatInterval: Long = 300000
    private var disable: Int = 1
    private lateinit var tempFolder: String
    private lateinit var attachName: String

    override fun execute(context: JobExecutionContext) {
        setParameters()
        if (disable == 0) {
            executeMonitorings()
            val trigger = TriggerBuilder.newTrigger()
            trigger.withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(repeatInterval).repeatForever()
            ).startAt(Date.from(Instant.now().plusMillis(repeatInterval))).withIdentity("monitoringSchedule")
            context.scheduler.rescheduleJob(context.trigger.key, trigger.build())
        }
    }

    private fun executeMonitorings() {
        val monitoringList: List<Monitoring>? = monitoringController.findMonitoringToRun()
        if(monitoringList.isNullOrEmpty()) return
        monitoringList.forEach { it.status = StatusMonitoring.RUNNING }
        monitoringController.saveAll(monitoringList.toList())
        monitoringList.forEach { monitoring ->
            val result = monitoringController.execute(monitoring)
            val attachment = File(tempFolder, "$attachName-${monitoring.name}.csv")
            if (!attachment.exists()) attachment.createNewFile()
            attachment.writer().append(result).flush()
            attachment.deleteOnExit()

            val sendNotifications =
                (monitoring.status == StatusMonitoring.NOK || monitoring.lastStatus == StatusMonitoring.NOK)

            if (sendNotifications) {
                if (monitoring.emailNotification) {
                    mailer.send(
                        mailTo = "${monitoring.mailTo}",
                        subject = "${monitoring.name} is ${monitoring.status}",
                        mailMessage = "${monitoring.title}\nDocumento -> ${monitoring.documentURL}\nConsulta\n${monitoring.databaseOneSql}",
                        attachment = attachment
                    )
                }
                if (monitoring.jiraNotification) {
                    if (!jiraApi.issueIsOpen("${monitoring.issue}") && monitoring.status == StatusMonitoring.NOK) {
                        monitoring.issue = jiraApi.createIssue(
                            title = "Monitoramento: ${monitoring.name}",
                            description = "${monitoring.title}\nDocumento -> ${monitoring.documentURL}\n${monitoring.databaseOneSql}",
                            attachment = attachment
                        ).toString()
                        monitoringController.save(monitoring)
                    }
                }
                if (monitoring.rocketNotification) {
                    rocketchat.sendMessage(monitoring)
                }
                if (monitoring.whatsappNotification && monitoring.errorCount == 1) {
                    // TODO Implement whatsapp groups
                }
            }
        }
    }

    private fun setParameters() {
        try {
            repeatInterval = parameterRepository.findByCode(Parameters.JOB_MONITORING_INTERVAL.toString())?.value.let {
                if (it.isNullOrEmpty()) "300000".toLong()
                else it.toLong()
            }
            tempFolder = parameterRepository.findByCode(Parameters.SYSTEM_TEMP_FOLDER.toString())?.value.toString()
            attachName =
                parameterRepository.findByCode(Parameters.JOB_MONITORING_ATTACHMENT_NAME.toString())?.value.toString()
            disable = parameterRepository.findByCode(Parameters.JOB_MONITORING_DISABLE.toString())?.value.let {
                if (it.isNullOrEmpty()) "1".toInt()
                else it.toInt()
            }
        } catch (e: Exception) {
            logController.automationLog("SET_PARAMETERS", e.stackTraceToString(), null)
        }
    }

    @Bean
    fun monitoringSchedule(): JobDetail? {
        return JobBuilder.newJob(MonitoringJob::class.java).withIdentity("monitoring").storeDurably(true)
            .withDescription("Agenda de execução de alertas").build()
    }

    @Bean
    fun monitoringTrigger(@Qualifier("monitoringSchedule") monitoringSchedule: JobDetail?): SimpleTriggerFactoryBean? {
        val trigger = SimpleTriggerFactoryBean()
        trigger.setJobDetail(monitoringSchedule!!)
        trigger.setRepeatInterval(repeatInterval)
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY)
        return trigger
    }

    @PostConstruct
    private fun init() {
        setParameters()
    }
}
