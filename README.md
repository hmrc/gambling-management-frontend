# gambling-management-frontend

## Project Title

Gambling Management Frontend

## Project Description

Gambling Management Frontend is a Scala 3 Play application that provides user-facing functionality for HMRC's Gambling Management services, complementing existing legacy interfaces.

This service follows HMRC frontend standards and integrates with backend services within the DASS platform.

## Tech Stack

Developed using Scala 3 with the Play Framework and suitable to run on JRE platform.

```
- Scala 3
- Play Framework 3
- Java 21+
- SBT
- HMRC bootstrap (play-30)
```

## Running the service

### Install/Update the Service Manager configuration (Prerequisite)
```
sm2 -update-config
```

### Run the whole stack under Service Manager

The service runs on port 10400. To start the service under service manager:
```
sm2 --start DASS_GAMBLING_ALL
```

To stop the service:
```
sm2 --stop DASS_GAMBLING_ALL
```

### To run the service locally from the console
```
sbt run
```

## Endpoints

### 1. Index

The service's landing page can be reached at 

http://localhost:10400/gambling-management-frontend

The application context is defined in `prod.routes`

```
GET         /gambling-management-frontend/
```

The application routes are defined in `app.routes`

```
GET        /
```

#### Behaviour

When the application is running:

```
Returns 200 
OK
```
and when the Service is Unavailable:

```
Returns 503 
The service cannot complete the health-check. 
```

### Build

### 1. To Format the files we are using 'scalafmt' code formatter

```
sbt scalafmtCheckAll scalafmtSbtCheck
```

This will verify formatting and fail if anything is unformatted

```
sbt scalafmtAll scalafmtSbt
```

This will:

Reformat all Scala source files
Reformat .sbt / project/*.scala
Make scalafmtCheck pass

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").