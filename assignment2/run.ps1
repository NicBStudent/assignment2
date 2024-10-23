Write-Host "1 GETClientTest"
Write-Host "2 AggregationServerTest"
Write-Host "3 AggregationServerThreadTest"
Write-Host "4 ContentServerTest"

$file_to_run = Read-Host 'Enter'

switch ($file_to_run) {
	"1" {
		java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore GETClientTest
	}
	"2" {
		java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore AggregationServerTest
	}
	"3" {
		java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore AggregationServerThreadTest
	}
	"4" {
		java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore ContentServerTest
	}
	default {
		java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore GETClientTest
	}
}