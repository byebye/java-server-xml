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
import java.util.List;
import java.util.stream.Collectors;

import static common.Settings.dateTimeFormatter;

public class MainWindow extends Application {

  private static final int SCENE_WIDTH = 1000;
  private static final int SCENE_HEIGHT = 600;

  private BorderPane mainPane;
  private TextArea areaFileEdit;
  private TableView<FileListEntry> tableFiles = new TableView<>();
  private TableColumn<FileListEntry, String> columnFilename = new TableColumn<>("Filename");
  private TableColumn<FileListEntry, String> columnSha = new TableColumn<>("SHA");
  private TableColumn<FileListEntry, String> columnModificationDate = new TableColumn<>("Modified");
  private TableColumn<FileListEntry, Button> columnOnServer = new TableColumn<>("On server");
  private TableColumn<FileListEntry, Button> columnOnClient = new TableColumn<>("On client");
  private ObservableList<FileListEntry> tableFilesData = FXCollections.observableArrayList();

  private Client client;

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

    TextField fieldFilename = new TextField();
    fieldFilename.setPrefWidth(270);
    fieldFilename.setPromptText("Enter filename");

    Button buttonSaveFile = new Button("Save");
    buttonSaveFile.setOnAction(event -> saveFile(fieldFilename.getText()));

    Button buttonUploadFile = new Button("Save & upload");
    buttonUploadFile.setOnAction(event -> {
      saveFile(fieldFilename.getText());
      uploadFile(fieldFilename.getText());
    });

    Button buttonSynchronize = new Button("Synchronize");
    buttonSynchronize.setOnAction(event -> {
      try {
        client.synchronizeWithServer();
      }
      catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Unable to synchronize files");
        alert.setHeaderText("Unable to synchronize files with server.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
      }
    });
    Button buttonRefresh = new Button("Refresh");
    buttonRefresh.setOnAction(event -> {
      System.err.println("Refreshing...");
      tableFilesData.clear();
      List<FileListEntry> localFiles = getLocalFilesList();
      System.err.println("Local files: " + localFiles);
      tableFilesData.addAll(localFiles);
      try {
        List<FileListEntry> serverFiles = client.getFilesList();
        System.err.println("Server files: " + serverFiles);
        tableFilesData.addAll(serverFiles);
      }
      catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Unable to get files list");
        alert.setHeaderText("Unable to get files list from server.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
      }
    });

    HBox hbox = new HBox(5.0, fieldFilename, buttonSaveFile, buttonUploadFile, buttonRefresh, buttonSynchronize);
    hbox.setPadding(new Insets(5, 5, 5, 5));
    mainPane.setBottom(hbox);
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
            buttonDownload = new Button("Yes");
            buttonDownload.setDisable(true);
          }
          else {
            buttonDownload = new Button("Download");
          }
          buttonDownload.setOnAction(event -> downloadFile(cellData.getValue().getFilename()));
          return new SimpleObjectProperty<>(buttonDownload);
        });
    columnOnServer.setCellValueFactory(
        cellData -> {
          Button buttonUpload;
          if (cellData.getValue().isOnServer()) {
            buttonUpload = new Button("Yes");
            buttonUpload.setDisable(true);
          }
          else {
            buttonUpload = new Button("Upload");
          }
          buttonUpload.setOnAction(event -> uploadFile(cellData.getValue().getFilename()));
          return new SimpleObjectProperty<>(buttonUpload);
        });
    tableFiles.getColumns().addAll(columnFilename, columnSha, columnModificationDate, columnOnClient, columnOnServer);
    mainPane.setRight(tableFiles);
  }

  private boolean downloadFile(String filename) {
    try {
      client.downloadFile(filename);
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
    }
    catch (IOException e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Unable to upload file");
      alert.setHeaderText("Unable to upload file '" + filename + "'.");
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }

  private List<FileListEntry> getLocalFilesList() {
    return FileUtils.listFiles(new File("."), new String[] { "xml" }, false)
        .stream()
        .map(file -> new FileListEntry(file.getName()))
        .collect(Collectors.toList());
  }

  private void saveFile(String filename) {
    File target = new File(filename);
    try {
      FileUtils.writeStringToFile(target, areaFileEdit.getText(), StandardCharsets.UTF_8);
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
