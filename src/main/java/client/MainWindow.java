package client;

import org.apache.commons.io.FileUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainWindow extends Application {

  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
  private ObservableList<FileListEntry> tableFilesData = FXCollections.observableArrayList(new FileListEntry("test.xml", LocalDateTime.now()));

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    mainPane = new BorderPane();
    mainPane.setPadding(new Insets(20, 20, 20, 20));

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

  private void initFileEditControls() {
    areaFileEdit = new TextArea();
    areaFileEdit.setPrefWidth(450);
    mainPane.setLeft(areaFileEdit);

    TextField fieldFilename = new TextField();
    fieldFilename.setPrefWidth(270);
    fieldFilename.setPromptText("Enter filename...");

    Button buttonSaveFile = new Button("Save");
    buttonSaveFile.setOnAction(event -> saveFile(fieldFilename.getText()));

    Button buttonUploadFile = new Button("Save & upload");
    buttonUploadFile.setOnAction(event -> {
      saveFile(fieldFilename.getText());
      uploadFile(fieldFilename.getText());
    });

    HBox hbox = new HBox(5.0, fieldFilename, buttonSaveFile, buttonUploadFile);
    mainPane.setBottom(hbox);
  }

  private void initTableFiles() {
    tableFiles.setItems(tableFilesData);
    tableFiles.setPrefWidth(550);
    tableFiles.setRowFactory(tableView -> {
      TableRow<FileListEntry> row = new TableRow<>();
      row.setOnMouseClicked(clickEvent -> {
        //        if (!row.isEmpty() && clickEvent.getClickCount() == 2) {
        //        }
      });
      return row;
    });
    columnFilename.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFilename()));
    columnSha.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getSha()));
    columnModificationDate.setCellValueFactory(
        cellData -> new SimpleObjectProperty<>(cellData.getValue().getModificationDate().format(dateTimeFormatter)));
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

  private void saveFile(String filename) {
    File target = new File(filename);
    try {
      FileUtils.writeStringToFile(target, areaFileEdit.getText(), Charset.defaultCharset());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void uploadFile(String filename) {

  }

  private void downloadFile(String filename) {

  }
}
