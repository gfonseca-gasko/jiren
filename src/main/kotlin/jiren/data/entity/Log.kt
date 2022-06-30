package jiren.data.entity

import java.sql.Timestamp
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity(name = "log")
@Table(name = "log")
class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long = 0

    @ManyToOne(optional = true, fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    var user: User? = null

    @Column(nullable = false)
    @NotNull
    var instant: Timestamp? = null
    var elapsedTime: Long? = null

    @Column(nullable = false)
    @NotBlank
    var code: String? = null

    @Lob
    var value: String? = null

    @Lob
    var sqlScript: String? = null
    var sqlType: String? = null
    var task: String? = null
}
