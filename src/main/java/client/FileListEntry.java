package client;

import java.time.LocalDateTime;

public class FileListEntry {
  private String filename;
  private String sha;
  private LocalDateTime modificationDate;
  private boolean onServer;
  private boolean onClient;

  public FileListEntry(String filename) {
    this(filename, "", null, false, true);
  }

  public FileListEntry(String filename, String sha, LocalDateTime modificationDate) {
    this(filename, "", modificationDate, true, false);
  }

  public FileListEntry(String filename, String sha, LocalDateTime modificationDate, boolean onServer,
      boolean onClient) {
    this.filename = filename;
    this.sha = sha;
    this.modificationDate = modificationDate;
    this.onServer = onServer;
    this.onClient = onClient;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getSha() {
    return sha;
  }

  public void setSha(String sha) {
    this.sha = sha;
  }

  public LocalDateTime getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(LocalDateTime modificationDate) {
    this.modificationDate = modificationDate;
  }

  public boolean isOnServer() {
    return onServer;
  }

  public void setOnServer(boolean onServer) {
    this.onServer = onServer;
  }

  public boolean isOnClient() {
    return onClient;
  }

  public void setOnClient(boolean onClient) {
    this.onClient = onClient;
  }

  @Override
  public String toString() {
    return filename;
//    return "FileListEntry{" +
//           "filename='" + filename + '\'' +
//           ", sha='" + sha + '\'' +
//           ", modificationDate=" + modificationDate +
//           ", onServer=" + onServer +
//           ", onClient=" + onClient +
//           '}';
  }
}
