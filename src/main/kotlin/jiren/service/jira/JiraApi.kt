package jiren.service.jira

import com.mashape.unirest.http.Unirest
import jiren.data.enum.Parameters
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.security.Credentials
import org.apache.http.entity.ContentType
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.io.File
import javax.annotation.PostConstruct

@Service
class JiraApi(
    private val parameterRepository: ParameterRepository,
    private val credentials: Credentials
) {
    private var user = ""
    private var token = ""
    private lateinit var projectKey: String
    private lateinit var jiraIssueType: String
    private lateinit var uri: String

    @PostConstruct
    fun init() {
        try {
            user = credentials.jiraCredentials.getString("jira-user")
            token = credentials.jiraCredentials.getString("jira-token")
        } catch (_: Exception) {}
        projectKey = "${parameterRepository.findByCode("${Parameters.JIRA_PROJECT}") ?: "ST"}"
        jiraIssueType = "${parameterRepository.findByCode("${Parameters.JIRA_ISSUE_TYPE}") ?: "Task"}"
        uri = "${parameterRepository.findByCode("${Parameters.JIRA_API_URI}") ?: "https://helpdeskmobly.atlassian.net/rest/api/2"}"
    }

    fun createIssue(title: String, description: String, attachment: File?): String? {
        val requestFields = JSONObject()
        val project = JSONObject()
        project.put("key", projectKey)
        requestFields.put("project", project)
        requestFields.put("summary", title)
        requestFields.put("description", description)
        val issueType = JSONObject()
        issueType.put("name", jiraIssueType)
        requestFields.put("issuetype", issueType)
        val payload = JSONObject()
        payload.put("fields", requestFields)

        val createIssueResponse = Unirest.post("$uri/issue/").basicAuth(user, token)
            .header("Content-Type", ContentType.APPLICATION_JSON.toString())
            .header("Accept", "application/json")
            .body(payload)
            .asJson()

        val createdIssueKey = createIssueResponse.body.`object`.getString("key")

        if (attachment != null && createIssueResponse.status == 201) {
            Unirest.post("$uri/issue/$createdIssueKey/attachments").basicAuth(user, token)
                .header("Accept", "application/json")
                .header("X-Atlassian-Token", "no-check")
                .field("file", attachment)
                .asJson()
        }
        return createdIssueKey
    }

    fun issueIsOpen(issueKey: String): Boolean {
        if (issueKey.isEmpty()) return false
        val response =
            Unirest.get("$uri/search?jql=key=$issueKey&fields=status").basicAuth(user, token)
                .header("Content-Type", ContentType.APPLICATION_JSON.toString())
                .header("Accept", "application/json").asJson()
        return try {
            val status = response.body.`object`
                .getJSONArray("issues")
                .getJSONObject(0)
                .getJSONObject("fields")
                .getJSONObject("status")
                .getString("name")
            (status != "Resolved" && status != "Reject" && status != "Rejected" && !status.isNullOrEmpty())
        } catch (e: Exception) {
            false
        }
    }

}