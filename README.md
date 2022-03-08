# MTASubwayStatus
A simple app that exposes endpoints letting you know if an MTA subway line is delayed and for how long relative to its uptime.

The project is well-commented, but here are some high level details:
  * The project makes use of the GTFS-RT API provided by the MTA
  * Exposes web endpoints at
    * /status -> which takes the name of a particular line as an argument and returns whether or not the line is currently delayed
    * /uptime -> which also takes the name of a particular line as an argument and returns the fraction of time that it has not been delayed since inception
  * The web server and API client run on separate threads using a thread-safe data structure to retain subway line info 
