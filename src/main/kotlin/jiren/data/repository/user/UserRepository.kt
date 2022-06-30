package jiren.data.repository.user

import jiren.data.entity.User
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepositoryImplementation<User, Long>, JpaSpecificationExecutor<User> {
    @Query("select u from user u where u.username = :name and u.enabled = true")
    fun findByUsername(name: String): User?

    @Query("select u from user u where u.enableShift = true and u.enabled = true")
    fun listPlantonyst(): List<User>?

    fun findByNameOrUsernameOrEmailOrDocument(name: String, username: String, email: String, document: String): User?
}