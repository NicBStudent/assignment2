ContentServer: GETClientTest
	javac ContentServer.java

GETClientTest: GETClient
	javac -cp ".:junit-4.13.2.jar:hamcrest-core-1.3.jar" GETClientTest.java

GETClient: AggregationServer
	javac GETClient.java

AggregationServer: AggregationServerThread
	javac AggregationServer.java

AggregationServerThread: AggregationServerThread.java
	javac AggregationServerThread.java
