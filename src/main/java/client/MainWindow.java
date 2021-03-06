package client;

import server.ThreadedServer;

import org.apache.commons.io.FileUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainWindow extends Application {

  public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final int SCENE_WIDTH = 1000;
  private static final int SCENE_HEIGHT = 600;

  private BorderPane mainPane;
  private TextArea areaFileEdit;
  private TableView<FileListEntry> tableFiles = new TableView<>();
  private TableColumn<FileListEntry, String> columnFilename = new TableColumn<>("Filename");
  private TableColumn<FileListEntry, String> columnSha = new TableColumn<>("SHA");
  private TableColumn<FileListEntry, String> columnModificationDate = new TableColumn<>("Modified");
  private TableColumn<FileListEntry, Button> columnOnClient = new TableColumn<>("On client");
  private ObservableList<FileListEntry> tableFilesData = FXCollections.observableArrayList();

  private Client client;
  private TextField fieldFilename;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    mainPane = new BorderPane();
    mainPane.setPadding(new Insets(20));

    initServerConnectControls();
    initFileEditControls();
    initTableFiles();

    Scene scene = new Scene(mainPane, SCENE_WIDTH, SCENE_HEIGHT);
    primaryStage.setScene(scene);
    primaryStage.setMaxWidth(SCENE_WIDTH);
    primaryStage.setMinWidth(SCENE_WIDTH);
    primaryStage.setMaxHeight(SCENE_HEIGHT);
    primaryStage.setMinHeight(SCENE_HEIGHT);
    mainPane.prefHeightProperty().bind(scene.heightProperty());
    mainPane.prefWidthProperty().bind(scene.heightProperty());

    primaryStage.setOnCloseRequest(e -> Platform.exit());
    primaryStage.setResizable(false);
    primaryStage.setTitle("Client");
    primaryStage.show();
  }

  private void initServerConnectControls() {
    TextField fieldServerAddress = new TextField("localhost");
    Label portLabel = new Label(":" + ThreadedServer.PORT);
    portLabel.setPadding(new Insets(5, 5, 5, 1));
    portLabel.setPrefHeight(fieldServerAddress.getPrefHeight());
    Label labelStatus = new Label("Not connected.");
    labelStatus.setPadding(new Insets(5));
    labelStatus.setPrefHeight(fieldServerAddress.getPrefHeight());
    Button buttonConnectToServer = new Button("Connect");
    buttonConnectToServer.setOnAction(event -> {
      try {
        if (client != null)
          client.close();
        client = new Client(new Socket(fieldServerAddress.getText(), ThreadedServer.PORT));
        labelStatus.setText("Connected!");
      }
      catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Connection error");
        alert.setHeaderText("Unable to connect to server.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
      }
    });
    HBox hbox = new HBox(fieldServerAddress, portLabel, buttonConnectToServer, labelStatus);
    hbox.setPadding(new Insets(5));
    mainPane.setTop(hbox);
  }

  private void initFileEditControls() {
    areaFileEdit = new TextArea();
    areaFileEdit.setPrefWidth(450);
    mainPane.setLeft(areaFileEdit);

    fieldFilename = new TextField();
    fieldFilename.setPrefWidth(270);
    fieldFilename.setPromptText("Enter filename");

    Button buttonSaveFile = new Button("Save");
    buttonSaveFile.setOnAction(event -> saveFile(fieldFilename.getText()));

    Button buttonUploadFile = new Button("Save & upload");
    buttonUploadFile.setOnAction(event -> {
      saveFile(fieldFilename.getText());
      uploadFile(fieldFilename.getText());
    });

    Button buttonDownloadAllFiles = new Button("Download all files");
    buttonDownloadAllFiles.setOnAction(event -> {
      try {
        for (FileListEntry fileEntry : tableFilesData) {
          if (fileEntry.isOnServer())
            client.downloadFile(fileEntry.getFilename());
        }
      }
      catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Unable to download files");
        alert.setHeaderText("Unable to download all files from server.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
      }
      refreshFilesList();
    });
    Button buttonUploadAllFiles = new Button("Upload all files");
    buttonUploadAllFiles.setOnAction(event -> {
      try {
        for (FileListEntry fileEntry : tableFilesData) {
          if (fileEntry.isOnClient())
            client.uploadFile(fileEntry.getFilename());
        }
      }
      catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Unable to upload files");
        alert.setHeaderText("Unable to upload all files to server.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
      }
      refreshFilesList();
    });
    Button buttonRefresh = new Button("Refresh files list");
    buttonRefresh.setOnAction(event -> refreshFilesList());

    HBox hbox = new HBox(5.0, fieldFilename, buttonSaveFile, buttonUploadFile, buttonRefresh, buttonDownloadAllFiles,
        buttonUploadAllFiles);
    hbox.setPadding(new Insets(5));
    mainPane.setBottom(hbox);
  }

  private void refreshFilesList() {
    System.err.println("Refreshing files list... ");
    tableFilesData.clear();
    Map<String, FileListEntry> filesMap = getLocalFilesList();
    System.err.println("Local files: " + filesMap.keySet());
    try {
      Set<FileListEntry> serverFiles = client.getFilesList();
      System.err.println("Server files: " + serverFiles);
      for (FileListEntry file : serverFiles) {
        if (filesMap.containsKey(file.getFilename())) {
          FileListEntry entry = filesMap.get(file.getFilename());
          entry.setOnServer(true);
          entry.setModificationDate(file.getModificationDate());
          entry.setSha(file.getSha());
        }
        else {
          filesMap.put(file.getFilename(), file);
        }
      }

      tableFilesData.addAll(
          filesMap.values().stream().sorted(Comparator.comparing(o -> o.getFilename())).collect(Collectors.toList()));
    }
    catch (Exception e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Unable to get server files list");
      alert.setHeaderText("Unable to get files list from server.");
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }

  private void initTableFiles() {
    tableFiles.setItems(tableFilesData);
    tableFiles.setPrefWidth(550);
    tableFiles.setMaxWidth(Double.MAX_VALUE);
    tableFiles.setRowFactory(tableView -> {
      TableRow<FileListEntry> row = new TableRow<>();
      row.setOnMouseClicked(clickEvent -> {
        if (!row.isEmpty() && clickEvent.getClickCount() == 2) {
          FileListEntry item = row.getItem();
          if (!item.isOnClient() && !downloadFile(item.getFilename()))
            return;
          try {
            String fileContent = FileUtils.readFileToString(new File(item.getFilename()), StandardCharsets.UTF_8);
            areaFileEdit.setText(fileContent);
            fieldFilename.setText(item.getFilename());
          }
          catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unable to open file");
            alert.setHeaderText("Unable to open file '" + item.getFilename() + "'.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
          }
        }
      });
      return row;
    });
    columnFilename.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFilename()));
    columnSha.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getSha()));
    columnModificationDate.setCellValueFactory(
        cellData -> {
          LocalDateTime modificationDate = cellData.getValue().getModificationDate();
          return new SimpleObjectProperty<>(modificationDate == null ? "" : modificationDate.format(dateTimeFormatter));
        });
    columnOnClient.setCellValueFactory(
        cellData -> {
          Button buttonDownload;
          if (cellData.getValue().isOnClient()) {
            if (cellData.getValue().isOnServer())
              buttonDownload = new Button("Yes - overwrite");
            else {
              buttonDownload = new Button("Yes");
              buttonDownload.setDisable(true);
            }
          }
          else {
            buttonDownload = new Button("No - download");
          }
          buttonDownload.setOnAction(event -> downloadFile(cellData.getValue().getFilename()));
          return new SimpleObjectProperty<>(buttonDownload);
        });
    tableFiles.getColumns().addAll(columnFilename, columnSha, columnModificationDate, columnOnClient);
    mainPane.setRight(tableFiles);
  }

  private boolean downloadFile(String filename) {
    try {
      client.downloadFile(filename);
      refreshFilesList();
    }
    catch (IOException e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Unable to download file");
      alert.setHeaderText("Unable to download file '" + filename + "'.");
      alert.setContentText(e.getMessage());
      alert.showAndWait();
      return false;
    }
    return true;
  }

  private void uploadFile(String filename) {
    try {
      client.uploadFile(filename);
      refreshFilesList();
    }
    catch (Exception e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Unable to upload file");
      alert.setHeaderText("Unable to upload file '" + filename + "'.");
      alert.setContentText(e.toString());
      alert.showAndWait();
    }
  }

  private Map<String, FileListEntry> getLocalFilesList() {
    return FileUtils.listFiles(new File("."), new String[] { "xml" }, false)
        .stream()
        .map(file -> new FileListEntry(file.getName()))
        .collect(Collectors.toMap(FileListEntry::getFilename, Function.identity()));
  }

  private void saveFile(String filename) {
    File target = new File(filename);
    try {
      FileUtils.writeStringToFile(target, areaFileEdit.getText(), StandardCharsets.UTF_8);
      if (!tableFilesData.stream().anyMatch(entry -> entry.getFilename().equals(filename)))
        tableFilesData.add(new FileListEntry(filename));
    }
    catch (IOException e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Unable to save file");
      alert.setHeaderText("Unable to save file as '" + filename + "'.");
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }
}
