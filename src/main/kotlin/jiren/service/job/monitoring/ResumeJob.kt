package jiren.service.job.monitoring

import jiren.service.mailer.SpringMail
import jiren.data.entity.Monitoring
import jiren.data.enum.Parameters
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.controller.LogController
import jiren.service.controller.MonitoringController
import org.quartz.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.annotation.PostConstruct

@Service
@DisallowConcurrentExecution
class ResumeJob(
    private val monitoringController: MonitoringController,
    private val parameterRepository: ParameterRepository,
    private val logController: LogController,
    private val mailer: SpringMail
) : Job {
    private var repeatInterval: Long = 300000
    private var disable: Int = 1
    private lateinit var mailTo: String
    private lateinit var subject: String
    private lateinit var template: String

    override fun execute(context: JobExecutionContext) {
        setParameters()
        if (disable == 0) {
            sendResume()
            val trigger = TriggerBuilder.newTrigger()
            trigger.withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(repeatInterval).repeatForever()
            ).startAt(Date.from(Instant.now().plusMillis(repeatInterval))).withIdentity("monitoringResume")
            context.scheduler.rescheduleJob(context.trigger.key, trigger.build())
        }
    }

    @Bean
    fun monitoringResume(): JobDetail? {
        return JobBuilder.newJob(ResumeJob::class.java).withIdentity("monitoring-resume").storeDurably(true)
            .withDescription("Envio do resumo de alertas").build()
    }

    @Bean
    fun resumeTrigger(@Qualifier("monitoringResume") resume: JobDetail?): SimpleTriggerFactoryBean? {
        val trigger = SimpleTriggerFactoryBean()
        trigger.setJobDetail(resume!!)
        trigger.setRepeatInterval(repeatInterval)
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY)
        return trigger
    }

    @PostConstruct
    private fun init() {
        setParameters()
    }

    private fun setParameters() {
        try {
            mailTo = parameterRepository.findByCode(Parameters.JOB_MONITORING_RESUME_MAILTO.toString())?.value.toString()
            subject = parameterRepository.findByCode(Parameters.JOB_MONITORING_RESUME_SUBJECT.toString())?.value.toString()
            template = parameterRepository.findByCode(Parameters.JOB_MONITORING_RESUME_TEMPLATE.toString())?.value.toString()
            disable =
                parameterRepository.findByCode(Parameters.JOB_MONITORING_RESUME_DISABLE.toString())?.value.let {
                    if(it.isNullOrEmpty()) "1".toInt()
                    else it.toInt()
                }
            repeatInterval =
                parameterRepository.findByCode(Parameters.JOB_MONITORING_RESUME_INTERVAL.toString())?.value.let {
                    if(it.isNullOrEmpty()) "300000".toLong()
                    else it.toLong()
                }
        } catch (e: Exception) {
            logController.automationLog("SET_PARAMETERS", e.stackTraceToString(), null)
        }
    }

    private fun sendResume() {
        try {
            val monitoringList: List<Monitoring>? = monitoringController.findPanelItens()
            if (monitoringList != null) mailer.sendWithTemplate(monitoringList as ArrayList<*>, template, mailTo, subject)
        } catch (e: Exception) {
            logController.monitoringLog("Resume", e.stackTraceToString())
        }
    }

}
