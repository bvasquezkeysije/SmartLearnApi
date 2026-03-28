# Script actualizado para levantar SmartLearnApi localmente

# 1. Ir al directorio del proyecto
E:
cd \SMARTLEARN\SmartLearnApi

# 2. Construir el proyecto sin tests
.\mvnw.cmd -DskipTests clean package

# 3. Configurar variables de entorno para la base de datos y el puerto
$env:DB_URL = 'jdbc:postgresql://localhost:5432/smartlearn'
$env:DB_USERNAME = 'bvasquezkeysije'
$env:DB_PASSWORD = '76636255ADK'
$env:PORT = '8080'

# 4. Levantar la API con Java
& 'C:\Program Files\Java\jdk-25.0.2\bin\java.exe' -Xms128m -Xmx512m -XX:+UseG1GC -jar .\target\SmartLearnApi-0.0.1-SNAPSHOT.jar
