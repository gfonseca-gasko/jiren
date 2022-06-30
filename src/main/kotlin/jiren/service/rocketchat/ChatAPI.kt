package jiren.service.rocketchat

import jiren.data.entity.Monitoring
import jiren.data.enum.Parameters
import jiren.data.enum.StatusMonitoring
import jiren.data.repository.parameter.ParameterRepository
import org.apache.http.entity.ContentType
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class ChatAPI(private val parameterRepository: ParameterRepository) {

    fun sendMessage(monitoring: Monitoring, appendable: String = "") {
        val webHookURI = "${parameterRepository.findByCode("${Parameters.ROCKETCHAT_WEBHOOK}")?.value}"
        val message = StringBuilder()
        message.append(
            "${monitoring.name.uppercase()} ${
                if (monitoring.status == StatusMonitoring.OK) "está Normalizado :white_check_mark:"
                else "está Crítico :setonfire:"
            }"
        )
        message.append("\nDocumentação -> ${monitoring.documentURL}")
        if (!monitoring.issue.isNullOrEmpty()) message.append("\nChamado -> ${monitoring.issue}")
        message.append("\nContagem de Disparos -> ${monitoring.errorCount}")
        if (appendable.isNotEmpty()) message.append(appendable)

        val json = JSONObject()
        json.put("roomId", "${monitoring.rocketchatRoom}")
        json.put("text", "$message")

        val request = HttpRequest.newBuilder().uri(URI.create(webHookURI)).timeout(Duration.ofMinutes(5))
            .header("Content-Type", "${ContentType.APPLICATION_JSON}")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build()
        HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendMessage(message: String, roomID: String = "") {
        val webHookURI = "${parameterRepository.findByCode("${Parameters.ROCKETCHAT_WEBHOOK}")?.value}"
        val json = JSONObject()
        json.put("roomId", roomID)
        json.put("text", message)
        val request = HttpRequest.newBuilder().uri(URI.create(webHookURI)).timeout(Duration.ofMinutes(5))
            .header("Content-Type", "${ContentType.APPLICATION_JSON}")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build()
        HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())
    }
}