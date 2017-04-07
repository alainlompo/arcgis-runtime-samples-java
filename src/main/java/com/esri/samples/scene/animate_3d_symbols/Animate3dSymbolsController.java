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

package com.esri.samples.scene.animate_3d_symbols;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.symbology.Renderer.SceneProperties;
import com.esri.arcgisruntime.symbology.RotationType;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

/**
 * Controller class. Automatically instantiated when the FXML loads due to the fx:controller attribute.
 */
public class Animate3dSymbolsController {
  // injected elements from fxml
  @FXML private CameraModel cameraModel;
  @FXML private AnimationModel animationModel;
  @FXML private PlaneModel planeModel;
  @FXML private SceneView sceneView;
  @FXML private MapView mapView;
  @FXML private ComboBox<String> missionSelector;
  @FXML private ToggleButton playButton;
  @FXML private ToggleButton followButton;
  @FXML private Timeline animation;

  private Camera camera;
  private Graphic plane3D;
  private Graphic plane2D;
  private List<Map<String, Object>> missionData;
  private Graphic routeGraphic;

  private static final String POSITION = "POSITION";
  private static final String HEADING = "HEADING";
  private static final String PITCH = "PITCH";
  private static final String ROLL = "ROLL";
  private static final SpatialReference WGS84 = SpatialReferences.getWgs84();
  private static final String ELEVATION_IMAGE_SERVICE =
      "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

  /**
   * Called after FXML loads. Sets up scene and map and configures property bindings.
   */
  public void initialize() {

    try {
      // create a scene
      ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
      sceneView.setArcGISScene(scene);

      // set initial camera viewpoint
      camera = new Camera(-111.8568649, 36.05793612, 2000, 10.0, 80.0, 300.0);
      sceneView.setViewpointCamera(camera);

      // add elevation data
      Surface surface = new Surface();
      surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
      scene.setBaseSurface(surface);

      // create a graphics overlay for the scene
      GraphicsOverlay sceneOverlay = new GraphicsOverlay();
      sceneOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
      sceneView.getGraphicsOverlays().add(sceneOverlay);

    //[DocRef: Name=Working_With_3D-Add_Graphics-Expression
      // create renderer to handle updating plane rotation using the GPU
      SimpleRenderer renderer3D = new SimpleRenderer();
      renderer3D.setRotationType(RotationType.GEOGRAPHIC);
      SceneProperties renderProperties = renderer3D.getSceneProperties();
      renderProperties.setHeadingExpression("[HEADING]");
      renderProperties.setPitchExpression("[PITCH]");
      renderProperties.setRollExpression("[ROLL]");
      sceneOverlay.setRenderer(renderer3D);
    //[DocRef: Name=Working_With_3D-Add_Graphics-Expression

      // set up mini map
      ArcGISMap map = new ArcGISMap(Basemap.createImagery());
      mapView.setMap(map);

      // create a graphics overlay for the mini map
      GraphicsOverlay mapOverlay = new GraphicsOverlay();
      mapView.getGraphicsOverlays().add(mapOverlay);

      // create renderer to handle updating plane heading using the graphics card
      SimpleRenderer renderer2D = new SimpleRenderer();
      renderer2D.setRotationExpression("[ANGLE]");
      mapOverlay.setRenderer(renderer2D);

      // set up route graphic
      SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF0000, 2);
      routeGraphic = new Graphic();
      routeGraphic.setSymbol(routeSymbol);
      mapOverlay.getGraphics().add(routeGraphic);

      // create 2D and 3D plane graphics
      plane2D = create2DPlane();
      mapOverlay.getGraphics().add(plane2D);
      plane3D = create3DPlane();
      sceneOverlay.getGraphics().add(plane3D);

      // setup animation to render a new frame every 20 ms by default
      animation.getKeyFrames().add(new KeyFrame(Duration.millis(20), e -> animate(animationModel.nextKeyframe())));

      // bind button properties
      followButton.disableProperty().bind(playButton.selectedProperty().not());
      followButton.textProperty().bind(Bindings.createStringBinding(() -> followButton.isSelected() ?
          "Free cam" : "Follow", followButton.selectedProperty()));
      playButton.textProperty().bind(Bindings.createStringBinding(() -> playButton.isSelected() ?
          "Stop" : "Play", playButton.selectedProperty()));

      // disable scroll zoom and dragging in follow mode
      EventHandler<Event> handler = (e) -> {
        if (!followButton.isDisabled() && cameraModel.isFollowing()) {
          e.consume();
        }
      };
      sceneView.addEventFilter(ScrollEvent.ANY, handler);
      sceneView.addEventFilter(MouseEvent.ANY, handler);

      // open default mission selection
      changeMission();

    } catch (Exception e) {
      // on any exception, print the stack trace
      e.printStackTrace();
    }
  }

  //[DocRef: Name=Working_With_3D-Add_Graphics-Model_Symbol
  /**
   * Creates a 3D graphic representing the plane in the scene.
   *
   * @throws URISyntaxException if model cannot be loaded
   */
  private Graphic create3DPlane() throws URISyntaxException {
    // load the plane's 3D model symbol
    String modelURI = new File("./samples-data/bristol/Collada/Bristol.dae").getAbsolutePath();
    ModelSceneSymbol plane3DSymbol = new ModelSceneSymbol(modelURI, 1.0);
    plane3DSymbol.loadAsync();

    // create the graphic
    return new Graphic(new Point(0, 0, 0, WGS84), plane3DSymbol);
  }
//[DocRef: Name=Working_With_3D-Add_Graphics-Model_Symbol

  /**
   * Creates a 2D graphic representing the plane on the mini map. Adds the graphic to the map view's graphics overlay.
   */
  private Graphic create2DPlane() {
    // create a blue (0xFF0000FF) triangle symbol to represent the plane on the mini map
    SimpleMarkerSymbol plane2DSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.TRIANGLE, 0xFF0000FF, 10);

    // create a graphic with the symbol and attributes
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("[ANGLE]", 0f);
    return new Graphic(new Point(0, 0, WGS84), attributes, plane2DSymbol);
  }

  /**
   * Called when a new mission is selected from the dropdown.
   */
  @FXML
  private void changeMission() {

    // clear previous mission data
    missionData = new ArrayList<>();

    // get mission data
    String mission = missionSelector.getSelectionModel().getSelectedItem();
    missionData = getMissionData(mission);
    animationModel.setFrames(missionData.size());
    animationModel.setKeyframe(0);

    // draw mission route on mini map
    PointCollection points = new PointCollection(WGS84);
    points.addAll(missionData.stream().map(m -> (Point) m.get(POSITION)).collect(Collectors.toList()));
    Polyline route = new Polyline(points);
    routeGraphic.setGeometry(route);

    // refresh mini map zoom and show initial keyframe
    mapView.setViewpointScaleAsync(100000).addDoneListener(() -> Platform.runLater(() -> animate(0)));
    animation.stop();

    // enable play button
    playButton.setSelected(false);
    playButton.setDisable(false);
  }

  /**
   * Loads the mission data from a .csv file into memory.
   *
   * @param mission .csv file name containing the mission data
   * @return ordered list of mapped key value pairs representing coordinates and rotation parameters for each step of
   * the mission
   */
  private List<Map<String, Object>> getMissionData(String mission) {

    // open a file reader to the mission file that automatically closes after read
    try (BufferedReader missionFile = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream
        ("/csv/" + mission)))) {
      List<Map<String, Object>> missionData = new ArrayList<>();
      missionFile.lines()
          //ex: -156.3666517,20.6255059,999.999908,83.77659,1.05E-09,-47.766567
          .map(l -> l.split(","))
          .map(l -> {
            // create a map of parameters (ordinates) to values
            Map<String, Object> ordinates = new HashMap<>();
            ordinates.put(POSITION, new Point(Double.valueOf(l[0]), Double.valueOf(l[1]), Double.valueOf(l[2]), WGS84));
            ordinates.put(HEADING, Double.valueOf(l[3]));
            ordinates.put(PITCH, Double.valueOf(l[4]));
            ordinates.put(ROLL, Double.valueOf(l[5]));
            return ordinates;
          })
          .collect(Collectors.toCollection(() -> missionData));

      return missionData;
    } catch (IOException e) {
      e.printStackTrace();
    }
    throw new RuntimeException("Error reading mission file: " + mission);
  }

  /**
   * Animates a single keyframe corresponding to the index in the mission data profile. Updates the position and
   * rotation of the 2D/3D plane graphic and sets the camera viewpoint.
   *
   * @param keyframe index in mission data to show
   */
  private void animate(int keyframe) {

    // get the next POSITION
    Map<String, Object> datum = missionData.get(keyframe);
    Point position = (Point) datum.get(POSITION);

    // update the model bean with new parameters
    planeModel.setAltitude(position.getZ());
    planeModel.setHeading((double) datum.get(HEADING));
    planeModel.setPitch((double) datum.get(PITCH));
    planeModel.setRoll((double) datum.get(ROLL));

    // move 2D plane to next POSITION
    plane2D.setGeometry(position);

    // move 3D plane to next POSITION
    plane3D.setGeometry(position);
    // update attribute expressions to immediately update rotation
    plane3D.getAttributes().put(HEADING, planeModel.getHeading());
    plane3D.getAttributes().put(PITCH, planeModel.getPitch());
    plane3D.getAttributes().put(ROLL, planeModel.getRoll());

    if (cameraModel.isFollowing()) {
      // move the camera to follow the plane
      camera = new Camera(position, cameraModel.getDistance(), planeModel.getHeading(), cameraModel.getAngle(),
          planeModel.getRoll());
      sceneView.setViewpointCamera(camera);

      // rotate the map view about the direction of motion
      mapView.setViewpoint(new Viewpoint(position, mapView.getMapScale(), 360 + planeModel.getHeading()));
    } else {
      plane2D.getAttributes().put("[ANGLE]", 360 + planeModel.getHeading() - mapView.getMapRotation());
    }
  }

  /**
   * Switches the animation on or off depending on the toggled state of the play button.
   */
  @FXML
  private void togglePlay() {

    if (playButton.isSelected()) {
      animation.play();
    } else {
      animation.stop();
    }
  }

  /**
   * Switches the toggle mode on the camera model when the toggle button is clicked.
   */
  @FXML
  private void toggleFollow() {

    if (followButton.isSelected()) {
      plane2D.getAttributes().put("[ANGLE]", 0f);
    }
    cameraModel.setFollowing(followButton.isSelected());
  }

  /**
   * Sets the map view scale to zoom in one exponential step.
   */
  @FXML
  private void zoomInMap() {
    mapView.setViewpoint(new Viewpoint((Point) plane2D.getGeometry(), mapView.getMapScale() / 5));
  }

  /**
   * Sets the map view scale to zoom out one exponential step.
   */
  @FXML
  private void zoomOutMap() {
    mapView.setViewpoint(new Viewpoint((Point) plane2D.getGeometry(), mapView.getMapScale() * 5));
  }

  /**
   * Stops the animation and disposes of application resources.
   */
  void terminate() {

    animation.stop();
    if (sceneView != null) {
      sceneView.dispose();
    }
    if (mapView != null) {
      mapView.dispose();
    }
  }
}
