package com.narakeet;
import java.util.Objects;

public class Example {


  public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
    String apiKey = Objects.requireNonNull(System.getenv("NARAKEET_API_KEY"), "NARAKEET_API_KEY environment variable is not set");

    String mainSourceFile = "source.txt";
    String videoDirectory = "video";

    VideoApi api = new VideoApi(apiKey);

    // upload the files to Narakeet
    String zipFile = api.zipDirectoryIntoTempFile(videoDirectory);
    VideoApi.UploadToken uploadToken = api.requestUploadToken();
    api.uploadZipFile(uploadToken, zipFile);

    // start the build task and wait for it to finish
    VideoApi.BuildTask buildTask = api.requestBuildTask(uploadToken, mainSourceFile);
    VideoApi.BuildTaskStatus taskResult = api.pollUntilFinished(buildTask, buildTaskStatus -> {
      // do something more useful here
      System.out.println("Progress: " + buildTaskStatus.getMessage() + " (" + buildTaskStatus.getPercent() + "%)" );
    });

    // grab the results
    if (taskResult.isSucceeded()) {
      String filePath = api.downloadToTempFile(taskResult.getResult());
      System.out.println("Downloaded to " + filePath);
    } else {
      System.out.println("Error creating video " + taskResult.getMessage());
    }
  }
}

