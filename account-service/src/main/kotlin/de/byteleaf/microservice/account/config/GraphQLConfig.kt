package de.byteleaf.microservice.account.config

import com.apollographql.federation.graphqljava.Federation
import com.apollographql.federation.graphqljava._Entity
import com.apollographql.federation.graphqljava.tracing.FederatedTracingInstrumentation
import de.byteleaf.microservice.account.control.AccountService
import de.byteleaf.microservice.account.entity.Account
import graphql.TypeResolutionEnvironment
import graphql.execution.instrumentation.Instrumentation
import graphql.kickstart.tools.SchemaParser
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.stream.Collectors

@Configuration
class GraphQLConfig {

    companion object {
        const val TYPENAME = "Account"
    }

    @Bean
    fun customSchema(schemaParser: SchemaParser, accountService: AccountService): GraphQLSchema {
        return Federation.transform(schemaParser.makeExecutableSchema())
            .fetchEntities { env: DataFetchingEnvironment ->
                env.getArgument<List<Map<String, Any>>>(_Entity.argumentName)
                    .stream()
                    .map<Any?> { values: Map<String, Any> ->
                        if (TYPENAME == values["__typename"]) {
                            val id = values["id"]
                            if (id is String) {
                                return@map accountService.account(id)
                            }
                        }
                        null
                    }
                    .collect(Collectors.toList())
            }
            .resolveEntityType { env: TypeResolutionEnvironment ->
                val src = env.getObject<Any>()
                if (src is Account) {
                    return@resolveEntityType env.schema.getObjectType(TYPENAME)
                }
                null
            }
            .build()
    }

    @Bean
    fun addFederatedTracing(): Instrumentation {
        return FederatedTracingInstrumentation(FederatedTracingInstrumentation.Options(true))
    }
}
