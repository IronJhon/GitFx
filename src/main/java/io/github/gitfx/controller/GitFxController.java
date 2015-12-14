/**
 * Copyright 2015 GitFx
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.gitfx.controller;

import io.github.gitfx.dialog.GitFxDialog;
import io.github.gitfx.dialog.GitFxDialogResponse;
import io.github.gitfx.GitFxApp;
import io.github.gitfx.GitResourceBundle;
import io.github.gitfx.data.GitRepoMetaData;
import io.github.gitfx.data.ProjectData;
import io.github.gitfx.data.RepositoryData;
import io.github.gitfx.util.GitFXGsonUtil;
import io.github.gitfx.util.WorkbenchUtil;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.event.EventHandler;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.layout.Pane;

public class GitFxController implements Initializable {

    Logger logger = LoggerFactory.getLogger(GitFxApp.class.getName());

    @FXML
    private Button gitclone;
    @FXML
    private Button gitsettings;
    @FXML
    private MenuItem gitSyncEverything;
    @FXML
    private Button gitinit;
    @FXML
    private Button gitopen;
    @FXML
    private MenuItem gitSpecific;
    @FXML
    private MenuButton gitsync;
    @FXML
    private TreeView<String> RepositoryTree;
    @FXML
    private Accordion historyAccordion;
    @FXML
    private Label commits;
    @FXML
    private AnchorPane treeContainer;
    @FXML
    private AnchorPane historyContainer;

    private ProgressIndicator pi;
    private GitFxDialog dialog;
    // Reference to the main application
    private GitFxApp gitFxApp;
    private ResourceBundle resourceBundle;

    /**
     * Reference to GitFxApp.
     *
     * @param gitFxApp
     */
    public void setMainApp(GitFxApp gitFxApp) {
        this.gitFxApp = gitFxApp;
        resourceBundle = new GitResourceBundle().getBundle();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Font.loadFont(GitFxController.class.getResource("/fonts/fontawesome-webfont.ttf").toExternalForm(), 12);
        gitinit.setText('\uf04b' + "");
        gitopen.setText('\uf07c' + "");
        gitsettings.setText('\uf013' + "");
        gitsync.setText('\uf021' + "");
        gitclone.setText('\uf0c5' + "");
        RepositoryTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    TreeItem<String> selectedItem = (TreeItem<String>) newValue;
                    if (selectedItem != null) {
                        logger.debug("Selected Text" + selectedItem.getValue());
                        addProgressIndicator(historyContainer);
                        if (!selectedItem.getValue().equals("github"))
                            initializeHistoryAccordion(selectedItem.getValue());
                    }
                });
        GitFXGsonUtil.checkRepoInformation();
        initializeTree();
        initializeHistoryAccordion();
    }
    /*
     * Method which initializes the Repository Tree Panel
     * Possible clients calls from
     * 1) Applicaiton Initialize event
     * 2) Initialization of a New Repository
     */

    private void initializeTree() {
        RepositoryData metaData = GitFXGsonUtil.getRepositoryMetaData();
        if (metaData != null) {
            TreeItem<String> treeRoot = new TreeItem<>(metaData.getServerName());
            treeRoot.setExpanded(true);
            List<ProjectData> projectData = metaData.getRepositories();
            projectData.stream().forEach((project) -> {
                treeRoot.getChildren().add(new TreeItem<>(project.getProjectName()));
            });
            RepositoryTree.setShowRoot(true);
            RepositoryTree.setRoot(treeRoot);
        }
    }

    //Method adds a progress indicator to a container of type Pane
    private void addProgressIndicator(Pane container) {
        ProgressIndicator pi = new ProgressIndicator();
        container.getChildren().add(pi);
        pi.setLayoutX(container.getWidth() / 2 - 20);
        pi.setLayoutY(container.getHeight() / 2) ;
        logger.debug(String.valueOf(container.getWidth()));
        pi.setPrefSize(50, 50);
        delayProgressIndicator(pi);
    }

    //Method simulates a delay on the progress indicator
    private void delayProgressIndicator(ProgressIndicator pi) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pi.setVisible(true);
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                  logger.debug("Interruped Exception"+exception.getMessage());
                } finally {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            pi.setVisible(false);
                        }
                    });
                }
            }
        }).start();
    }

    /*
     * By default load the first repository in the first server listed in tree
     */
    private void initializeHistoryAccordion() {
        RepositoryData repoData = GitFXGsonUtil.getRepositoryMetaData();
        //If application is loaded for the first time the metaData will be missing
        if (repoData != null) {
            GitRepoMetaData metaData = GitFXGsonUtil.getGitRepositoryMetaData(repoData.getFirstRepoPath());
            initializeHistoryAccordion(metaData);
        }
    }

    /*
     * Given a projectName find the repoPath from the JSON and initialize the
     * History accordion accordingly
     */
    private void initializeHistoryAccordion(String projectName) {
        RepositoryData repoData = GitFXGsonUtil.getRepositoryMetaData();
        String repoPath = repoData.getRepoPath(projectName);
        GitRepoMetaData metaData = GitFXGsonUtil.getGitRepositoryMetaData(repoPath);
        //addProgressIndicator();
        initializeHistoryAccordion(metaData);
    }

    /*
     *  Given the GitRepoMetaData load the accordion with the repository 
     *  commit history
     */
    private void initializeHistoryAccordion(GitRepoMetaData metaData) {
        ObservableList<TitledPane> panes = historyAccordion.getPanes();
        if (panes != null) {
            historyAccordion.getPanes().removeAll(panes);
        }
        if(metaData == null){
        commits.setText("Empty Repository!");    
        }
        else{
        ArrayList<String> list = metaData.getShortMessage();
        ArrayList<ArrayList<String>> commitFiles = metaData.getCommitFiles();
        TitledPane pane;
        int i = 0;
        for (String str : list) {
            ListView<String> changedFiles = new ListView<>();
            changedFiles.autosize();
            changedFiles.setItems(FXCollections.observableArrayList(commitFiles.get(i++)));
            changedFiles.maxHeight(Double.MAX_VALUE);
            pane = new TitledPane(str, changedFiles);
            //Specifying a height here as without height repo's with more commits
            //were not expanding to show commits
            pane.setMaxHeight(50);
            historyAccordion.getPanes().add(pane);
        }
        //Show number of commits on top of history accordion
        commits.setText(metaData.getCommitCount() + " commits");
        }
        
    }

    @FXML
    public void onGitSettingsClicked(ActionEvent event) {
    }

    @FXML
    public void onGitCloneClicked(ActionEvent event) {
        dialog = new GitFxDialog();
        Pair<String, String> clonedRepo = dialog.gitCloneDialog(resourceBundle.getString("cloneRepo"),
                resourceBundle.getString("cloneRepo"),
                null);
        if (dialog.getResponse() == GitFxDialogResponse.CLONE && clonedRepo != null) {
            logger.debug("Response Ok Repo Path");
            String repoURL =clonedRepo.getKey();
            String repoName="";
            if(repoURL.contains(".git"))
             repoName = repoURL.substring(repoURL.lastIndexOf("/")+1,repoURL.indexOf(".git"));
            else
             repoName = repoURL.substring(repoURL.lastIndexOf("/")+1);   
            String localPath = clonedRepo.getValue();
            localPath = localPath +File.separator+repoName;
            logger.debug("Clone URL" + repoURL );
            logger.debug("Local Path" + localPath);
            if(GitFXGsonUtil.cloneGitRepository(repoURL,localPath)){
            GitRepoMetaData metaData = GitFXGsonUtil.getGitRepositoryMetaData(localPath);    
            initializeHistoryAccordion(metaData);
            GitFXGsonUtil.saveRepositoryInformation("github", repoName,localPath);
            initializeTree();
            }
            else{
                logger.debug("Its not a Valid URL");
            }
                
        } else {
            logger.debug("Response Cancel");
        }
    }

    @FXML
    public void onGitOpenClicked(ActionEvent event) {
        dialog = new GitFxDialog();
        String repoPath = dialog.gitOpenDialog(resourceBundle.getString("openRepo"),
                resourceBundle.getString("chooseRepo"),
                resourceBundle.getString("repo"));
        if (dialog.getResponse() == GitFxDialogResponse.OK) {
            logger.debug("Response Ok Repo Path" + repoPath);
            GitRepoMetaData metaData = GitFXGsonUtil.getGitRepositoryMetaData(repoPath);
            initializeHistoryAccordion(metaData);
            String repoName =repoPath.substring(repoPath.lastIndexOf(File.separator)+1);
            GitFXGsonUtil.saveRepositoryInformation("github", repoName,
                    repoPath);
            initializeTree();
        } else {
            logger.debug("Response Cancel");
        }
    }

    @FXML
    public void onGitSyncClicked(ActionEvent event) {
       logger.debug("Sync particular repository");
    }
    
    @FXML
    public void syncEveryThingClicked(ActionEvent event) {
        dialog = new GitFxDialog();
        dialog.gitConfirmationDialog(resourceBundle.getString("sync"),
                resourceBundle.getString("syncAll"),
                resourceBundle.getString("syncAllDesc"));
        if (dialog.getResponse() == GitFxDialogResponse.OK) {
            logger.debug("Sync all clicked");
        } else {
            logger.debug("Cancelled");
        }
    }

    @FXML
    public void onGitInitClicked(ActionEvent event) {
        dialog = new GitFxDialog();
        Pair<String, String> newRepo = dialog.gitInitDialog(resourceBundle.getString("initRepo"),
                resourceBundle.getString("initRepo"),
                null);
        if (dialog.getResponse() == GitFxDialogResponse.INITIALIZE && newRepo != null) {
            logger.debug("Git init clicked");
            logger.debug("Project Name" + newRepo.getKey());
            logger.debug("Local Path" + newRepo.getValue());

            String path = WorkbenchUtil.getGitFxWorkbenchPath();
            GitFXGsonUtil.saveRepositoryInformation("github", newRepo.getKey(),
                    newRepo.getValue());
            //Call a utility method to intitilize a repository
            initializeTree();
        } else {
            logger.debug("Cancelled");
        }
    }

    @FXML
    public void onGitParticularRepositoryClicked(ActionEvent event) {
        logger.debug("Git Particular Repository clicked");
        dialog = new GitFxDialog();
        RepositoryData metaData = GitFXGsonUtil.getRepositoryMetaData();
        List<String> list = new ArrayList<>();
        if(metaData!=null){  
        List<ProjectData> projectData = metaData.getRepositories();
            projectData.stream().forEach((project) -> 
                list.add(project.getProjectName())
            );
         }
        dialog.gitFxInformationListDialog(resourceBundle.getString("syncRepo"), resourceBundle.getString("repo"),null,list);
        
                
    }

}
