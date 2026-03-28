# SmartLearnApi - README

## Resumen
SmartLearnApi es el backend REST de SmartLearn.
Se encarga de autenticacion, usuarios, examenes, cursos, soporte, roles y comparticion.

## Framework y stack usado
- Framework principal: Spring Boot 4.0.3
- Lenguaje: Java 21
- Build tool: Maven (mvn / mvnw)
- Seguridad: Spring Security + JWT (jjwt)
- Persistencia: Spring Data JPA + Hibernate
- Base de datos: PostgreSQL
- Migraciones: Flyway
- Documentacion API: springdoc OpenAPI (Swagger UI)
- Librerias extra: Apache POI (Excel), PDFBox (PDF)

## Dependencias principales (pom.xml)
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-security
- postgresql
- flyway-core
- flyway-database-postgresql
- springdoc-openapi-starter-webmvc-ui
- jjwt-api / jjwt-impl / jjwt-jackson

## Estructura base
- `src/main/java/com/bardales/SmartLearnApi/controller` -> endpoints REST
- `src/main/java/com/bardales/SmartLearnApi/service` -> logica de negocio
- `src/main/java/com/bardales/SmartLearnApi/domain` -> entidades y repositorios
- `src/main/java/com/bardales/SmartLearnApi/dto` -> contratos request/response
- `src/main/resources/application.properties` -> configuracion
- `src/main/resources/db/migration` -> scripts Flyway

## Comandos utiles
- Compilar: `mvn clean compile`
- Ejecutar local: `mvn spring-boot:run`
- Migrar schema: `mvn -DskipTests flyway:migrate`

## Runtime recomendado
- Java 21
- Maven 3.9+
- PostgreSQL 16+ (proyecto probado tambien en PostgreSQL 18)
