package jiren.data.entity

import jiren.data.enum.HttpAllowedMethods
import jiren.data.enum.MonitoringType
import jiren.data.enum.StatusMonitoring
import java.sql.Timestamp
import java.time.Instant.now
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity(name = "monitoring")
@Table(name = "monitoring")
class Monitoring {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @NotBlank
    @Column(nullable = false, unique = true)
    var name: String = ""

    var title: String = ""

    @NotNull
    var executionInterval: Int = 999

    @NotNull
    var errorCount: Int = 0

    @NotNull
    var enabled: Boolean = false

    @NotNull
    var showInPanel: Boolean = false

    @NotNull
    var emailNotification: Boolean = false

    @NotNull
    var rocketNotification: Boolean = false

    @NotNull
    var whatsappNotification: Boolean = false

    @NotNull
    var jiraNotification: Boolean = false

    @NotNull
    @Enumerated(EnumType.STRING)
    var type: MonitoringType? = null

    @Enumerated(EnumType.STRING)
    var status: StatusMonitoring? = null
        set(status: StatusMonitoring?) {
            lastStatus = field
            field = status
        }

    @Enumerated(EnumType.STRING)
    var lastStatus: StatusMonitoring? = null

    @Enumerated(EnumType.STRING)
    var httpType: HttpAllowedMethods? = null

    @ManyToOne
    @JoinColumn(nullable = true)
    var databaseOptionOne: Database? = null

    @ManyToOne
    @JoinColumn(nullable = true)
    var databaseOptionTwo: Database? = null

    @NotNull
    var scheduleAt: Timestamp? = null
    var databaseOneSql: String = ""
    var databaseTwoSql: String = ""
    var body: String = ""
    var mailTo: String? = null
    var rocketchatRoom: String? = null
    var documentURL: String? = null
    var issue: String? = null
    var firstReport: Timestamp? = null
    var lastReport: Timestamp? = null
    var ranAt: Timestamp? = null
    var httpResponseCode: String? = null
    var httpTimeout: String? = null
    var httpRequestUrl: String? = null
    var httpContentType: String? = null

    fun onSuccess() {
        status = StatusMonitoring.OK
        firstReport = null
        lastReport = null
        ranAt = Timestamp.from(now())
        errorCount = 0
    }

    fun onError() {
        status = StatusMonitoring.NOK
        if (firstReport == null) firstReport = Timestamp.from(now())
        lastReport = Timestamp.from(now())
        ranAt = Timestamp.from(now())
        errorCount++
    }

    fun reschedule() {
        scheduleAt = Timestamp.from(now().plusMillis((executionInterval * 60 * 1000).toLong()))
    }

    fun setScheduleAt(scheduleAt: LocalDateTime?) {
        if (scheduleAt != null) this.scheduleAt = Timestamp.valueOf(scheduleAt)
    }

    fun getScheduleAt(): LocalDateTime? {
        return scheduleAt?.toLocalDateTime()
    }
}


