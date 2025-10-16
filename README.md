# Dropbox Business API Assignment (Java 8 + Maven)

## Prerequisites
- Java 8 (JDK 1.8)
- Maven 3.x
- Dropbox Business account (team admin) to create a test app

## Setup — Create a Dropbox Team App
1. Go to Dropbox App Console → Create App → **Scoped access** → **Team (Dropbox Business API)**.
2. Add Redirect URI: `http://localhost:45678/callback`
3. Enable scopes: `team_info.read`, `members.read`, `events.read`.
4. Note App Key (client id) and App Secret.

## Local setup
2. Edit `config.properties` and set:

## Build 
- mvn clean package
- Run (after filling config.properties):
- java -jar target/cloudeagle-dropbox-api-assignment-1.0.0.jar

