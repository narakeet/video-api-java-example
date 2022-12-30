package com.narakeet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class VideoApi {
  private final String apiKey;
  private final String apiUrl;
  private final int pollingIntervalSeconds;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UploadToken {
    private String url;
    private String contentType;
    private String repositoryType;
    private String repository;
    public String getUrl() {
      return url;
    }
    public void setUrl(String url) {
      this.url = url;
    }
    public String getContentType() {
      return contentType;
    }
    public void setContentType(String contentType) {
      this.contentType = contentType;
    }
    public String getRepository() {
      return repository;
    }
    public void setRepository(String repository) {
      this.repository = repository;
    }
    public String getRepositoryType() {
      return repositoryType;
    }
    public void setRepositoryType(String repositoryType) {
      this.repositoryType = repositoryType;
    }
  }

  public static class BuildTaskRequest {
    private String repositoryType;
    private String repository;
    private String source;

    public BuildTaskRequest(String repositoryType, String repository, String source) {
      this.repositoryType = repositoryType;
      this.repository = repository;
      this.source = source;
    }
    public BuildTaskRequest(UploadToken uploadToken, String source) {
      this(uploadToken.getRepositoryType(), uploadToken.getRepository(), source);
    }
    public String getRepositoryType() {
      return repositoryType;
    }
    public String getRepository() {
      return repository;
    }
    public String getSource() {
      return source;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BuildTask {
    private String statusUrl;
    private String taskId;

    public String getStatusUrl() {
      return statusUrl;
    }
    public void setStatusUrl(String statusUrl) {
      this.statusUrl = statusUrl;
    }
    public String getTaskId() {
      return taskId;
    }
    public void setTaskId(String taskId) {
      this.taskId = taskId;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BuildTaskStatus {
    private String message;
    private int percent;
    private boolean succeeded;
    private boolean finished;
    private String result;

    public String getMessage() {
      return message;
    }
    public void setMessage(String message) {
      this.message = message;
    }
    public int getPercent() {
      return percent;
    }
    public void setPercent(int percent) {
      this.percent = percent;
    }
    public boolean isSucceeded() {
      return succeeded;
    }
    public void setSucceeded(boolean succeeded) {
      this.succeeded = succeeded;
    }
    public boolean isFinished() {
      return finished;
    }
    public void setFinished(boolean finished) {
      this.finished = finished;
    }
    public String getResult() {
      return result;
    }
    public void setResult(String result) {
      this.result = result;
    }
  }


  public VideoApi(String apiKey, String apiUrl, int pollingIntervalSeconds) {
    this.apiKey = apiKey;
    this.apiUrl = apiUrl;
    this.pollingIntervalSeconds = pollingIntervalSeconds;
  }

  public VideoApi(String apiKey) {
    this(apiKey, "https://api.narakeet.com", 5);
  }
  public UploadToken requestUploadToken() throws IOException, JsonProcessingException {
    String url = apiUrl + "/video/upload-request/zip";
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(url);
    request.addHeader("x-api-key", apiKey);
    HttpResponse response = client.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 300) {
      String responseString = EntityUtils.toString(response.getEntity());
      throw new IOException("HTTP error: " + statusCode + " " + responseString);
    }
    String responseString = EntityUtils.toString(response.getEntity());
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(responseString, UploadToken.class);
  }

  public String zipDirectoryIntoTempFile(String directory) throws IOException {
    String tempPath = System.getProperty("java.io.tmpdir");
    String tempFileName = Paths.get(tempPath, UUID.randomUUID().toString() + ".zip").toString();
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(tempFileName)))) {
      Files.walk(Paths.get(directory))
        .filter(p -> !Files.isDirectory(p))
        .forEach(p -> {
          try {
            ZipEntry zipEntry = new ZipEntry(p.getFileName().toString());
            zos.putNextEntry(zipEntry);
            Files.copy(p, zos);
            zos.closeEntry();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    }
    return tempFileName;
  }
  public void uploadZipFile(UploadToken uploadToken, String zipFilePath) throws IOException {
    HttpClient client = HttpClientBuilder.create().build();
    HttpPut request = new HttpPut(uploadToken.getUrl());
    File zipFile = new File(zipFilePath);
    InputStream in = new FileInputStream(zipFile);
    HttpEntity entity = new InputStreamEntity(in, zipFile.length(), ContentType.parse(uploadToken.getContentType()));
    request.setEntity(entity);
    HttpResponse response = client.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 200 && statusCode < 300) {
      return;
    } else {
      String responseString = EntityUtils.toString(response.getEntity());
      throw new IOException("HTTP error: " + statusCode + " " + responseString);
    }
  }
  public BuildTask requestBuildTask(BuildTaskRequest buildTaskRequest) throws IOException {
    HttpClient client = HttpClientBuilder.create().build();
    HttpPost request = new HttpPost(apiUrl + "/video/build");
    request.addHeader("x-api-key", apiKey);
    request.addHeader("Content-Type", "application/json");

    ObjectMapper mapper = new ObjectMapper();
    String requestJson = mapper.writeValueAsString(buildTaskRequest);
    request.setEntity(new StringEntity(requestJson));
    HttpResponse response = client.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 200 && statusCode < 300) {
      HttpEntity responseEntity = response.getEntity();
      String responseJson = EntityUtils.toString(responseEntity);
      return mapper.readValue(responseJson, BuildTask.class);
    } else {
      String responseString = EntityUtils.toString(response.getEntity());
      throw new IOException("HTTP error: " + statusCode + " " + responseString);
    }
  }
  public BuildTask requestBuildTask(UploadToken uploadToken, String sourceFileInZip) throws IOException {
    return requestBuildTask(new BuildTaskRequest(uploadToken, sourceFileInZip));
  }  
  public BuildTaskStatus pollUntilFinished(BuildTask buildTask, Consumer<BuildTaskStatus> progressCallback) throws IOException {
    while (true) {
      HttpClient httpClient = HttpClientBuilder.create().build();
      try {
        HttpGet httpGet = new HttpGet(buildTask.getStatusUrl());
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
          String responseContent = EntityUtils.toString(response.getEntity());
          ObjectMapper objectMapper = new ObjectMapper();
          BuildTaskStatus buildTaskStatus = objectMapper.readValue(responseContent, BuildTaskStatus.class);
          if (buildTaskStatus.isFinished()) {
            return buildTaskStatus;
          }
          if (progressCallback != null) {
            progressCallback.accept(buildTaskStatus);
          }
        } else {
          String responseString = EntityUtils.toString(response.getEntity());
          throw new IOException("HTTP error: " + statusCode + " " + responseString);
        }
      } finally {
        httpClient.getConnectionManager().shutdown();
      }
      try {
        Thread.sleep(pollingIntervalSeconds * 1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  public BuildTaskStatus pollUntilFinished(BuildTask buildTask) throws IOException {
    return pollUntilFinished(buildTask, null);
  }
  public String downloadToTempFile(String url) throws IOException {
    HttpClient httpClient = HttpClientBuilder.create().build();
    HttpGet httpGet = new HttpGet(url);
    HttpResponse response = httpClient.execute(httpGet);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 200 && statusCode < 300) {
      String tempPath = System.getProperty("java.io.tmpdir");
      File tempFile = new File(tempPath, UUID.randomUUID().toString() + ".mp4");
      FileOutputStream outputStream = new FileOutputStream(tempFile);
      response.getEntity().writeTo(outputStream);
      outputStream.close();
      return tempFile.getAbsolutePath();
    } else {
      String responseString = EntityUtils.toString(response.getEntity());
      throw new IOException("HTTP error: " + statusCode + " " + responseString);
    }
  }
}
