package jiren.data.entity

import jiren.data.enum.StatusUser
import org.springframework.lang.Nullable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@Entity(name = "user")
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false)
    var enabled: Boolean = false

    @NotBlank
    @Column(nullable = false)
    var name: String = ""

    @Column(nullable = true, unique = true)
    @NotBlank
    @Email
    var email: String = ""

    @NotBlank
    @Column(nullable = false, unique = true)
    var username: String = ""

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.MERGE], targetEntity = Role::class, optional = true)
    @Nullable
    var role: Role? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var status: StatusUser? = null

    @NotBlank
    @Column(unique = true)
    var document: String? = null
    var phone: String? = null
    var chat: String? = null
    var changePassword: Boolean = true
    var enableShift: Boolean = false

    @NotBlank
    @Column(nullable = false)
    var password: String? = null
        set(password) {
            if (password != null) field = BCryptPasswordEncoder().encode(password)
        }

    override fun toString(): String {
        return name
    }
}