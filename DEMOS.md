# Demo index

One row per generated demo. `BRD.md` has changed over time, so each demo was
built against the requirements snapshotted in its own folder rather than the
current ones. Telemetry and OpenAPI were requirements that were later removed,
which is why the newer demos mostly lack them.

An em dash means the demo does not have that feature. Persistence names the API
the demo chose and the datastore behind it.

| Demo                                                                                    | Stack                     | Persistence                                | Telemetry                     | OpenAPI                |
|-----------------------------------------------------------------------------------------|---------------------------|--------------------------------------------|-------------------------------|------------------------|
| [`jakarta-ee-demo-bob-claude-opus-4-5_1`](jakarta-ee-demo-bob-claude-opus-4-5_1/)       | Jakarta EE / Open Liberty | Jakarta Data (over JPA) / Derby (embedded) | —                             | —                      |
| [`jakarta-ee-demo-bob-claude-opus-4-5_2`](jakarta-ee-demo-bob-claude-opus-4-5_2/)       | Jakarta EE / Open Liberty | Jakarta Persistence (JPA) / PostgreSQL     | MicroProfile Telemetry        | MicroProfile OpenAPI   |
| [`jakarta-ee-demo-bob-claude-opus-4-5_3`](jakarta-ee-demo-bob-claude-opus-4-5_3/)       | Jakarta EE / Open Liberty | Jakarta Data (over JPA) / PostgreSQL       | MicroProfile Telemetry †      | MicroProfile OpenAPI † |
| [`jakarta-ee-demo-claude-code-opus-5_1`](jakarta-ee-demo-claude-code-opus-5_1/)         | Jakarta EE / Open Liberty | Jakarta Persistence (JPA) / PostgreSQL     | MicroProfile Telemetry        | MicroProfile OpenAPI   |
| [`jakarta-ee-demo-claude-code-opus-5_2`](jakarta-ee-demo-claude-code-opus-5_2/)         | Jakarta EE / Open Liberty | Jakarta Data (over JPA) / PostgreSQL       | MicroProfile Telemetry        | MicroProfile OpenAPI   |
| [`jakarta-ee-demo-claude-code-opus-5_3`](jakarta-ee-demo-claude-code-opus-5_3/)         | Jakarta EE / Open Liberty | Jakarta Data (over JPA) / Derby (embedded) | MicroProfile Telemetry †      | MicroProfile OpenAPI † |
| [`jakarta-ee-demo-copilot-cli-sonnet-5-5_1`](jakarta-ee-demo-copilot-cli-sonnet-5-5_1/) | Jakarta EE / Open Liberty | Jakarta Persistence (JPA) / PostgreSQL     | MicroProfile Telemetry        | MicroProfile OpenAPI   |
| [`jakarta-ee-demo-copilot-cli-sonnet-5-5_2`](jakarta-ee-demo-copilot-cli-sonnet-5-5_2/) | Jakarta EE / Open Liberty | Jakarta Data (over JPA) / PostgreSQL       | MicroProfile Telemetry †      | MicroProfile OpenAPI   |
| [`java-demo-bob-claude-opus-4-5_1`](java-demo-bob-claude-opus-4-5_1/)                   | Spring Boot               | Spring Data JPA / PostgreSQL               | Micrometer, OpenTelemetry SDK | springdoc / Swagger UI |
| [`java-demo-bob-claude-opus-4-5_2`](java-demo-bob-claude-opus-4-5_2/)                   | Spring Boot               | Spring Data JPA / PostgreSQL               | Micrometer, OpenTelemetry SDK | springdoc / Swagger UI |
| [`java-demo-bob-claude-opus-4-5_3`](java-demo-bob-claude-opus-4-5_3/)                   | Spring Boot               | Spring Data JPA / PostgreSQL               | —                             | springdoc / Swagger UI |
| [`java-demo-bob-claude-opus-4-5_4`](java-demo-bob-claude-opus-4-5_4/)                   | Spring Boot               | Spring Data JPA / PostgreSQL               | —                             | —                      |
| [`java-demo-bob-claude-opus-4-5_5`](java-demo-bob-claude-opus-4-5_5/)                   | Spring Boot               | in-memory                                  | —                             | —                      |
| [`java-demo-claude-code-opus-5_1`](java-demo-claude-code-opus-5_1/)                     | Spring Boot               | Spring Data JPA / PostgreSQL               | Micrometer, OpenTelemetry SDK | springdoc / Swagger UI |
| [`java-demo-claude-code-opus-5_2`](java-demo-claude-code-opus-5_2/)                     | Spring Boot               | Spring JDBC / PostgreSQL                   | Micrometer, OpenTelemetry SDK | springdoc / Swagger UI |
| [`java-demo-claude-code-opus-5_3`](java-demo-claude-code-opus-5_3/)                     | Spring Boot               | Spring Data JPA / PostgreSQL               | —                             | —                      |
| [`java-demo-claude-code-opus-5_4`](java-demo-claude-code-opus-5_4/)                     | Spring Boot               | Spring JDBC / PostgreSQL                   | —                             | —                      |
| [`java-demo-copilot-cli-sonnet-5-5_1`](java-demo-copilot-cli-sonnet-5-5_1/)             | Spring Boot               | Spring JDBC / PostgreSQL                   | Micrometer, OpenTelemetry SDK | springdoc / Swagger UI |
| [`java-demo-copilot-cli-sonnet-5-5_2`](java-demo-copilot-cli-sonnet-5-5_2/)             | Spring Boot               | Spring Data JPA / PostgreSQL               | —                             | springdoc / Swagger UI |
| [`java-demo-copilot-cli-sonnet-5-5_3`](java-demo-copilot-cli-sonnet-5-5_3/)             | Javalin                   | plain JDBC / PostgreSQL                    | —                             | —                      |
| [`java-demo-copilot-cli-sonnet-5-5_4`](java-demo-copilot-cli-sonnet-5-5_4/)             | Spring Boot               | Spring JDBC / PostgreSQL                   | —                             | —                      |
| [`java-demo-copilot-cli-sonnet-5-5_5`](java-demo-copilot-cli-sonnet-5-5_5/)             | Spring Boot               | Spring JDBC / PostgreSQL                   | —                             | —                      |

† Present only because `server.xml` enables the `microProfile-7.1` convenience
feature, which installs MicroProfile OpenAPI and Telemetry as a side effect. The
capability is live, but the app adds no annotations, configuration, or
instrumentation of its own, so this was not a deliberate choice.
