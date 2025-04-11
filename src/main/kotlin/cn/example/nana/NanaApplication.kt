package cn.example.nana

import org.springframework.ai.autoconfigure.vectorstore.cassandra.CassandraVectorStoreAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableCassandraRepositories("cn.example.nana.models.cassandra")
@EnableScheduling
@SpringBootApplication(exclude = [CassandraVectorStoreAutoConfiguration::class])
class NanaApplication

fun main(args: Array<String>) {
    runApplication<NanaApplication>(*args)
}
