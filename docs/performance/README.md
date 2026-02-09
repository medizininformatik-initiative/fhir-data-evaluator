# Manual Performance Test

## Generate Test Data
To generate test data, for example this [synthea test data generator](https://github.com/samply/blaze/tree/master/docs/performance/synthea) can be used.

## Run Test
```sh
sh docs/performance/average-time-test.sh <measure.json> <output/dir> <iterations> <docker-image>
```
This test will run the Fhir Data Evaluator multiple times, save the MeasureReports to the output directory and read the
evaluation time from an extension in the MeasureReports to calculate the average and standard deviation. If the test script is run a second time, it will remove the old MeasureReports.

## Output

The Test will print to stdout the average processing time (in seconds) and the standard deviation it took for the Fhir 
Data Evaluator to evaluate the Measure. It will look something like this:
```
"average","standard deviation"
26.915481,0.090585
```

## Build

To apply changes made to the java code, the Fhir Data Evaluator has to be build manually:

* Maven Build: 
```sh 
mvn clean install -DskipTests
```
* Docker Build:
```sh
docker build -f ./docker/Dockerfile -t fhir-data-evaluator .
```
* Run Test with custom image:
```sh
sh docs/performance/average-time-test.sh ${PWD}/docs/example-measures/example-measure-2.json ${PWD}/target/performance-output 2 fhir-data-evaluator
```
