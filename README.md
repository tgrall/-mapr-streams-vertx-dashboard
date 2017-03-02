
# MapR Streams & Vert.x Sample Application

This simple project shows how you can use [Vert.x](http://vertx.io/) to consume data from MapR Streams.
The data are send using the Vert.x bus from the server to the browser using Websockets.

### Prerequisites

* Java Development Kit 8
* Maven 3.3 or later
* Mapr cluster with MapR Streams


## Build and Run the application

**Configuration**

Create a MapR Streams and Topic

```
$ maprcli stream create -path /apps/iot_stream -produceperm p -consumeperm p -topicperm p


$ maprcli stream topic create -path /apps/iot_stream -topic ecg
```


**Build the Application**

```
cd mapr-streams-vertx-dashboard

mvn clean package
```

**Run the Application**

Open 2 terminal windows and run the following command:

Web Application (Vert.x application)

```
$ java -jar ./target/mapr-streams-vertx-dashboard-1.0-SNAPSHOT-fat.jar web 8080 /apps/iot_stream:ecg
```

Open your browser and go to [http://localhost:8080](http://localhost:8080) and click in the menu to see the different views.

Generate ECG data on the topic

```
$ java -jar ./target/mapr-streams-vertx-dashboard-1.0-SNAPSHOT-fat.jar data /apps/iot_stream:ecg ./data/ecg.tsv
```

The Web application should show graph with the data coming from the MapR Stream topic.


## Tips & Technique

To send data to the browser, this application use the Vert.x bus, see the code in `com.mapr.demo.WebServer`. 
The `main()` method creates the Web Server and initialize the bus exposed as a Web socket.

In the browser you just need to add some Javascript to received event from the bus see for example the `log.html`, 
and the javascript at the bottom of the page.


## Credits

This application uses:

* [Bootstrap](http://getbootstrap.com/)
* [Smoothies Charts](http://smoothiecharts.org/)
* [D3JS](https://d3js.org/) & * [JKE D3 ECG](https://github.com/joakimkemeny/jke.d3.ecg) from Joakim Kemeny