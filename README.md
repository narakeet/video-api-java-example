# Narakeet Video Build API example in Java

This repository provides a quick example demonstrating how to access the Narakeet [markdown to video API](https://www.narakeet.com/docs/automating/rest/) from java.

The example sends a request to generate a video from a local ZIP file (it creates the zip file from the contents of the [video](video) directory, then downloads the resulting video into a local temporary file. 

## Prerequisites

To use this example, you will need Java 11 or later, and Maven 3 or later, and an API key for Narakeet.

The example uses the [`org.apache.http`](https://hc.apache.org/) library to execute HTTPS requests.

## Running the example

1. Set a local environment variable called `NARAKEET_API_KEY`, containing your API key (or modify [src/main/java/com/narakeet/Example.java](src/main/java/com/narakeet/Example.java) line 13 to include your API key).
2. optionally modify [src/main/java/com/narakeet/Example.java](src/main/java/com/narakeet/Example.java) lines 10, 11 and 24 to change the video source files location and the progress function.
2. run `mvn install` to download the dependencies
3. run `mvn compile exec:java` to run the conversion.

## More information

Check out <https://www.narakeet.com/docs/automating/rest/> for more information on the Narakeet Markdown to Video API. 

