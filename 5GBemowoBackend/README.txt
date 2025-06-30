# Spring Boot Backend (Kotlin)

## How to run

1. Install Java 21
   https://adoptium.net/en-GB/temurin/releases/

2. You must have Python available in the terminal under the command `python3` and all Python libraries listed in the `requirements.txt` file (located one level above) must be installed. You can use the command:
   pip install -r ../requirements.txt

3. Create configuration files – they are not included in the project and must be created manually:
    PROJECT STRUCTURE AT THE BOTTOM IF NEEDED!

   a) Create `application.properties` file in:
      `src/main/resources/application.properties` (obligatory)

   Example content (you MUST replace values with your owns):

   spring.application.name=5GBemowo-Backend

   spring.datasource.url=jdbc:postgresql://localhost:5432/YOUR_DATABASE
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   spring.datasource.driver-class-name=org.postgresql.Driver

   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   spring.jpa.hibernate.ddl-auto=update

   spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration

   spring.mail.host=smtp.YOUR_MAIL_PROVIDER.com
   spring.mail.port=587
   spring.mail.username=YOUR_EMAIL
   spring.mail.password=YOUR_EMAIL_PASSWORD
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   spring.mail.properties.mail.smtp.starttls.required=true
   spring.mail.default-encoding=UTF-8
   spring.mail.properties.mail.smtp.connectiontimeout=5000
   spring.mail.properties.mail.smtp.timeout=5000
   spring.mail.properties.mail.smtp.writetimeout=5000

   google.api.key=YOUR_GOOGLE_API_KEY
   google.api.model=YOUR_MODEL
   google.api.baseurl=https://YOUR_GOOGLE_ENDPOINT

   openai.api.key=YOUR_OPENAI_API_KEY

   b) Create `application-docker.yml` file in (obligatory for docker):
      `src/main/resources/application-docker.yml`

   Example content:

   spring:
     application:
       name: 5GBemowo-Backend

     datasource:
       url: jdbc:postgresql://db:5432/YOUR_DATABASE
       username: postgres
       password: ${DB_PASSWORD}
       driver-class-name: org.postgresql.Driver

     jpa:
       database-platform: org.hibernate.dialect.PostgreSQLDialect
       hibernate:
         ddl-auto: update

     mail:
       host: smtp.YOUR_MAIL_PROVIDER.com
       port: 587
       username: YOUR_EMAIL
       password: YOUR_EMAIL_PASSWORD
       properties:
         mail:
           smtp:
             auth: true
             starttls:
               enable: true
               required: true
             connectiontimeout: 5000
             timeout: 5000
             writetimeout: 5000
       default-encoding: UTF-8

   google:
     api:
       key: YOUR_GOOGLE_API_KEY
       model: YOUR_MODEL
       baseurl: https://YOUR_GOOGLE_ENDPOINT

   openai:
     api:
       key: YOUR_OPENAI_API_KEY

4. In the terminal, in catalog 5GBemowoMerged\5GBemowoBackend run:
   ./gradlew build

5. Then start the backend in the same catalog as in 4th point:
   ./gradlew bootRun

## Project structure

5GBemowoMerged/
├── 5GBemowoBackend/ <- You are here
│   └── src/main/resources/
│       ├── application.properties <- You must create this manually
│       └── application-docker.yml <- You must create this manually
├── 5GBemowoFrontend/
├── requirements.txt <- Required Python dependencies