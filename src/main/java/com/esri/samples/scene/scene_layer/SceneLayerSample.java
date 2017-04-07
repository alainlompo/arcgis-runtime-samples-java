/*
 * Copyright 2016 Esri.
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

package com.esri.samples.scene.scene_layer;

import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SceneLayerSample extends Application {

  private SceneView sceneView;

  @Override
  public void start(Stage stage) throws Exception {

    try {

      // create stack pane and JavaFX app scene
      StackPane stackPane = new StackPane();
      Scene fxScene = new Scene(stackPane);

      // set title, size, and add JavaFX scene to stage
      stage.setTitle("Scene Layer Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(fxScene);
      stage.show();

    //[DocRef: Name=Working_With_3d-Display_Scene-Create_SceneLayer
      // create a scene and add a basemap to it
      ArcGISScene scene = new ArcGISScene();
      scene.setBasemap(Basemap.createImagery());
      
   // add a scene layer
      String buildings = "http://scene.arcgis.com/arcgis/rest/services/Hosted/Buildings_Brest/SceneServer/layers/0";
       ArcGISSceneLayer sceneLayer = new ArcGISSceneLayer(buildings);
       scene.getOperationalLayers().add(sceneLayer);
     //[DocRef: Name=Working_With_3d-Display_Scene-Create_SceneLayer

     //[DocRef: Name=Working_With_3d-Display_Scene-Display_Scene
      // add the SceneView to the stack pane
      sceneView = new SceneView();
      sceneView.setArcGISScene(scene);
      stackPane.getChildren().addAll(sceneView);
    //[DocRef: Name=Working_With_3d-Display_Scene-Display_Scene

    //[DocRef: Name=Working_With_3d-Display_Scene-Elevation
      // add base surface for elevation data
      Surface surface = new Surface();
      String localElevationImageService = "http://scene.arcgis.com/arcgis/rest/services/BREST_DTM_1M/ImageServer";
      surface.getElevationSources().add(new ArcGISTiledElevationSource(localElevationImageService));
      scene.setBaseSurface(surface);
    //[DocRef: Name=Working_With_3d-Display_Scene-Elevation

    //[DocRef: Name=Working_With_3d-Display_Scene-Camera
      // add a camera and initial camera position (Brest, France)
      Camera camera = new Camera(48.37,-4.50, 1000.0, 10.0, 70, 0.0);
      sceneView.setViewpointCamera(camera);
    //[DocRef: Name=Working_With_3d-Display_Scene-Camera

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

    if (sceneView != null) {
      sceneView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }

}