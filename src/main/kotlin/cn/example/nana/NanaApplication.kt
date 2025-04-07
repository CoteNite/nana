package cn.example.nana

import org.springframework.ai.autoconfigure.vectorstore.cassandra.CassandraVectorStoreAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [CassandraVectorStoreAutoConfiguration::class]
)
class NanaApplication

fun main(args: Array<String>) {
    runApplication<NanaApplication>(*args)
}
