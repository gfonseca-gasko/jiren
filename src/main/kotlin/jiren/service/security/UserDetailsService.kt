@file:Suppress("CAST_NEVER_SUCCEEDS")

package jiren.service.security

import jiren.data.entity.Permission
import jiren.data.entity.Role
import jiren.data.repository.user.RoleRepository
import jiren.data.repository.user.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("userDetailsService")
@Transactional
class UserDetailsService(private val userRepository: UserRepository, private val roleRepository: RoleRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user: User = userRepository.findByUsername(username) as User? ?: return User(
            "", "", false, false, false, false, getAuthorities(
                roleRepository.findByName("USER")!!
            )
        )
        return User(
            user.username, user.password, user.isEnabled, true, true, true, getAuthorities(user.authorities as Role)
        )
    }

    private fun getAuthorities(
        role: Role
    ): Collection<GrantedAuthority> {
        return getGrantedAuthorities(getPrivileges(role))
    }

    private fun getPrivileges(role: Role): List<String> {
        val privileges: MutableList<String> = ArrayList()
        val collection: MutableList<Permission> = ArrayList()

        privileges.add(role.name)
        role.permissions.let { collection.addAll(it) }

        for (item in collection) {
            privileges.add(item.description ?: "")
        }
        return privileges
    }

    private fun getGrantedAuthorities(privileges: List<String>): List<GrantedAuthority> {
        val authorities: MutableList<GrantedAuthority> = ArrayList()
        for (privilege in privileges) {
            authorities.add(SimpleGrantedAuthority(privilege))
        }
        return authorities
    }

}