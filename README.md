# vicidial-cli

Simple CLI to interact with Vicidial. This README explains how to build, run, and troubleshoot the project. It also shows how to use a .env file for credentials and examples of common commands.

## Prerequisites
- Java 11+ (JDK installed)
- Maven
- A Vicidial instance and valid credentials

## Environment / Credentials
The CLI should not contain credentials in source. Use a `.env` file or environment variables.

Example `.env` (project root)
VICIDIAL_URL=http://localhost:8080
VICIDIAL_USER=admin
VICIDIAL_PASSWORD=secret

Add `.env` to `.gitignore` to avoid committing credentials:
```
.env
target/
```

Load `.env` into your shell (example for bash):
```
set -a && . .env && set +a
```
Or use a Java dotenv library to load `.env` automatically during runtime.

## Build
Build the project with Maven:
```
mvn clean package
```
By default, `target/vicidial-cli-1.0.0.jar` will be produced. If you run the jar with:
```
java -jar target/vicidial-cli-1.0.0.jar
```
and you get:
```
Exception in thread "main" java.lang.NoClassDefFoundError: picocli/CommandLine
```
This happens because the jar does not include required dependencies (e.g., picocli). There are two ways to fix that:

Option A — Create a shaded ("fat") jar (recommended):
Add the Maven Shade plugin to your `pom.xml` to bundle dependencies into a single executable jar (ensure `mainClass` points to your entrypoint `dev.pablo.cli.MainApplication`):
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals><goal>shade</goal></goals>
                    <configuration>
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>dev.pablo.cli.MainApplication</mainClass>
                            </transformer>
                        </transformers>
                        <finalName>vicidial-cli-1.0.0-shaded</finalName>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
Then build and run:
```
mvn clean package
java -jar target/vicidial-cli-1.0.0-shaded.jar --help
```

Option B — Run with dependencies on classpath:
```
mvn dependency:copy-dependencies
java -cp "target/vicidial-cli-1.0.0.jar:target/dependency/*" dev.pablo.cli.MainApplication --help
```

## Run / Usage
To see available commands:
```
java -jar target/vicidial-cli-1.0.0-shaded.jar --help
```
General CLI usage:
```
java -jar target/vicidial-cli-1.0.0-shaded.jar <command> [options]
```
Example:
```
java -jar target/vicidial-cli-1.0.0-shaded.jar getAllCampaigns
java -jar target/vicidial-cli-1.0.0-shaded.jar updateCred --name "test" --password "test123" TestCaller
```

## Troubleshooting
- NoClassDefFoundError (e.g., picocli.CommandLine): Use a shaded jar or run with dependencies in the classpath.
- Credentials not found: Ensure `.env` is loaded or variables are exported to your environment.
- Permission or port errors: Check that Vicidial is reachable and ports are open.