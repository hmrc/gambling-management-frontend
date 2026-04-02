
# gambling-management-frontend

## Project Title

Gambling Management Frontend

## Project Description

Gambling Management Frontend will complement HMRC's traditional screens.

## Requirements
```
Developed using Scala 3 with the Play Framework and suitable to run on JRE 21 or later.
```

## Running the service

### To run the whole stack under Service Manager
```
sm2 --start VAPING_STAMPS_API_ALL
```

### To control the service locally from the console
```
sbt run
```

To stop the service running:
```
sm2 --start DASS_GAMBLING_ALL
```

## Endpoints

### 1. Index

The service's landing page can be reached at 

```
GET /gambling-management-frontend/
```

Behaviour

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

## Build

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