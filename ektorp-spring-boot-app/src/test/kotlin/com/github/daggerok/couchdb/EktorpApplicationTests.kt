package com.github.daggerok.couchbase.officialcouchbasejavasdk

import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.ektorp.CouchDbConnector
import org.ektorp.CouchDbInstance
import org.ektorp.http.HttpClient
import org.ektorp.http.StdHttpClient
import org.ektorp.impl.StdCouchDbConnector
import org.ektorp.impl.StdCouchDbInstance
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import java.util.*

@ConfigurationProperties(prefix = "couchdb")
data class CouchDBProperties(
    val username: String? = "myUser",
    val password: String? = "myPassword",
    val dbName: String? = "myDbName",
    val host: String? = "127.0.0.1",
    val port: Int = 5984
)

@TestConfiguration
@EnableConfigurationProperties(CouchDBProperties::class)
class TestConfig {

    @Lazy @Bean
    fun httpClient(couchDB: CouchDBProperties): HttpClient =
        StdHttpClient.Builder()
            .host(couchDB.host)
            .port(couchDB.port)
            .username(couchDB.username)
            .password(couchDB.password)
            .cleanupIdleConnections(true)
            .connectionTimeout(5000)
            .maxConnections(10)
            .compression(true)
            .enableSSL(false)
            .build()

    @Lazy @Bean
    fun dbInstance(httpClient: HttpClient): CouchDbInstance =
        StdCouchDbInstance(httpClient)

    @Lazy @Bean
    fun db(dbInstance: CouchDbInstance, couchDB: CouchDBProperties): CouchDbConnector =
        dbInstance.createConnector(couchDB.dbName, true)
}

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = [TestConfig::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(ReplaceUnderscores::class)
class EktorpApplicationTests(@Autowired val db: CouchDbConnector) {

    companion object { val log = logger() }

    @Test
    fun `should work`() {
        // given
        val _id = UUID.randomUUID().toString()
        // when
        db.create(_id, mapOf("Hello" to "World"))
        // then
        val created = db.get(Map::class.java, _id)
        log.info { "created: $created" }
        // and
        assertThat(created).isNotNull
        assertThat(created["_id"]).isNotNull
        assertThat(created["_id"]).isEqualTo(_id)
        assertThat(created["_rev"]).isNotNull
        assertThat(created["Hello"]).isNotNull
        assertThat(created["Hello"]).isEqualTo("World")
    }
}
