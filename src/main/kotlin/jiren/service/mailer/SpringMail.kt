package jiren.service.mailer

import jiren.service.controller.LogController
import jiren.service.security.Credentials
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.internet.InternetAddress

@Service
class SpringMail(
    private val logController: LogController,
    private val templateEngine: SpringTemplateEngine,
    private val credentials: Credentials
) {
    private fun javaMailSenderImpl(): JavaMailSenderImpl {
        val javaMail = JavaMailSenderImpl()
        try {
            val username: String? = credentials.mailerCredentials.getString("mailer-user")
            val password: String? = credentials.mailerCredentials.getString("mailer-password")
            javaMail.host = "smtp.gmail.com"
            javaMail.protocol = "smtp"
            javaMail.port = 587
            javaMail.defaultEncoding = "UTF-8"
            javaMail.username = username
            javaMail.password = password
            javaMail.javaMailProperties.setProperty("mail.smtp.auth", "true")
            javaMail.javaMailProperties.setProperty("mail.smtp.starttls.enable", "true")
            javaMail.javaMailProperties.setProperty("mail.debug", "false")
        } catch (e: Exception) {
            logController.systemLog(this.javaClass.simpleName, e.stackTraceToString())
        }
        return javaMail
    }

    @Throws(MessagingException::class, IOException::class)
    fun sendWithTemplate(
        objects: ArrayList<*>,
        template: String,
        mailTo: String,
        subject: String
    ) {
        val emailSender = javaMailSenderImpl()
        val message = emailSender.createMimeMessage()
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo, false))
        val helper = MimeMessageHelper(
            message,
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name()
        )
        val context = Context()
        context.setVariable("objlist", objects)
        val html = templateEngine.process(template, context)
        helper.setText(html, true)
        helper.setSubject(subject)
        emailSender.send(message)
    }

    @Throws(MessagingException::class, IOException::class)
    fun send(
        mailTo: String,
        subject: String,
        mailMessage: String,
        attachment: File?
    ) {
        val emailSender = javaMailSenderImpl()
        val message = emailSender.createMimeMessage()
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo, false))
        val helper = MimeMessageHelper(
            message,
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name()
        )
        if (attachment != null) {
            helper.addAttachment(attachment.name, attachment)
        }
        helper.setText(mailMessage, false)
        helper.setSubject(subject)
        emailSender.send(message)
    }

}