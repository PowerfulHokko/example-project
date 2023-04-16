# example-project

Small project with Scala, Akka-http, Spray-Json & Slick (Don't ask about the combination, I just use what I know)

The idea is an Item Service for a webshop, with methods for the items, category and stock. 
I locally linked it to a posgres db, the dumpfile of that is in the resources folder. 

If questions... ask, and I shall answer (when I have the time, I might not be online a lot this week)

Also if it's something that gets propper shape it could be a fun project for other beginning Scala developers too: project based learning is awesome!

## How to run
___
1. Run `docker-compose up` from the root. This will setup a postgres database and PgAdmin on your local.
2. Compile the project using SBT: `sbt compile`
3. Run the project: `sbt run`. Alternatively, use IntelliJIdea to run the application.