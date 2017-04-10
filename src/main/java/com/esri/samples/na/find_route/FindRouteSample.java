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

package com.esri.samples.na.find_route;

import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DrawStatus;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol.Style;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol.HorizontalAlignment;
import com.esri.arcgisruntime.symbology.TextSymbol.VerticalAlignment;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionMessageType;
import com.esri.arcgisruntime.tasks.networkanalysis.PointBarrier;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class FindRouteSample extends Application {

  private MapView mapView;
  private RouteTask routeTask;
  private RouteParameters routeParameters;
  private ListView<String> directionsList = new ListView<>();

  private Graphic routeGraphic;
  private GraphicsOverlay routeGraphicsOverlay = new GraphicsOverlay();

  private final SpatialReference ESPG_3857 = SpatialReference.create(102100);
  private static final int WHITE_COLOR = 0xffffffff;
  private static final int BLUE_COLOR = 0xff0000ff;
  private static final int RED_COLOR = 0xffff0000;

  @Override
  public void start(Stage stage) throws Exception {

    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

      // set title, size, and add scene to stage
      stage.setTitle("Find Route Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      // create a control panel
      VBox vBoxControl = new VBox(6);
      vBoxControl.setMaxSize(200, 300);
      vBoxControl.getStyleClass().add("panel-region");

      Label directionsLabel = new Label("Route directions:");
      directionsLabel.getStyleClass().add("panel-label");

      // create buttons for user interaction
      Button findButton = new Button("Find route");
      findButton.setMaxWidth(Double.MAX_VALUE);
      findButton.setDisable(true);
      Button resetButton = new Button("Reset");
      resetButton.setMaxWidth(Double.MAX_VALUE);
      resetButton.setDisable(true);

      // find route
      findButton.setOnAction(e -> {
        //[DocRef: Name=Route_And_Directions-Find_Route-Solve
        try {
          RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
          //[DocRef: Name=Route_And_Directions-Find_Route-Solve
          //[DocRef: Name=Route_And_Directions-Find_Route-Display_Route
          List<Route> routes = result.getRoutes();
          if (routes.size() < 1) {
            directionsList.getItems().add("No Routes");
          }
          Route route = routes.get(0);
          routeGraphic = new Graphic(route.getRouteGeometry(), new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, BLUE_COLOR, 2));
          routeGraphicsOverlay.getGraphics().add(routeGraphic);
          //[DocRef: Name=Route_And_Directions-Find_Route-Display_Route

          // get route street names
          route.getDirectionManeuvers().stream().flatMap(mvr -> mvr.getManeuverMessages().stream()).filter(ms -> ms
              .getType().equals(DirectionMessageType.STREET_NAME)).forEach(st -> directionsList.getItems().add(st
                  .getText()));

          resetButton.setDisable(false);
          findButton.setDisable(true);

        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });

      // clear the route and the directions maneuver found
      resetButton.setOnAction(e -> {
        //[DocRef: Name=Display_Information-Graphics-Remove_Graphic
        routeGraphicsOverlay.getGraphics().remove(routeGraphic);
        //[DocRef: Name=Display_Information-Graphics-Remove_Graphic
        directionsList.getItems().clear();
        resetButton.setDisable(true);
        findButton.setDisable(false);
      });

      // add buttons and direction list and label to the control panel
      vBoxControl.getChildren().addAll(directionsLabel, directionsList, findButton, resetButton);

      // create a ArcGISMap with a streets basemap
      ArcGISMap map = new ArcGISMap(Basemap.createStreets());

      // set the ArcGISMap to be displayed in this view
      mapView = new MapView();
      mapView.setMap(map);

      // enable find a route button when mapview is done loading
      mapView.addDrawStatusChangedListener(e -> {
        if (e.getDrawStatus() == DrawStatus.COMPLETED) {
          findButton.setDisable(false);
        }
      });

      // set the viewpoint to San Diego (U.S.)
      mapView.setViewpointGeometryAsync(new Envelope(-13067866, 3843014, -13004499, 3871296, ESPG_3857));

      // add the graphic overlay to the map view
      mapView.getGraphicsOverlays().add(routeGraphicsOverlay);

      try {
        //[DocRef: Name=Route_And_Directions-Find_Route-Route_Task
        // create route task from San Diego service
        String routeTaskSanDiego =
            "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";
        routeTask = new RouteTask(routeTaskSanDiego);
        //[DocRef: Name=Route_And_Directions-Find_Route-Route_Task

        //[DocRef: Name=Route_And_Directions-Find_Route-Load_Task-Java
        // load route task
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {

          try {
            if (routeTask.getLoadError() == null && routeTask.getLoadStatus() == LoadStatus.LOADED) {
              //[DocRef: Name=Route_And_Directions-Find_Route-Load_Task-Java

              //[DocRef: Name=Route_And_Directions-Find_Route-Default_Parameters
              // get default route parameters
              routeParameters = routeTask.createDefaultParametersAsync().get();

              // set flags to return stops and directions
              routeParameters.setReturnStops(true);
              routeParameters.setReturnDirections(true);
              routeParameters.setOutputSpatialReference(ESPG_3857);
              //[DocRef: Name=Route_And_Directions-Find_Route-Default_Parameters

              //[DocRef: Name=Route_And_Directions-Find_Route-Stops_And_Barriers
              // set stop locations
              Point stop1Loc = new Point(-1.3018598562659847E7, 3863191.8817135547, ESPG_3857);
              Point stop2Loc = new Point(-1.3036911787723785E7, 3839935.706521739, ESPG_3857);

              // add route stops
              List<Stop> routeStops = routeParameters.getStops();
              routeStops.add(new Stop(stop1Loc));
              routeStops.add(new Stop(stop2Loc));

              // create barriers
              PointBarrier pointBarrier = new PointBarrier(new Point(-1.302759917994629E7, 3853256.753745117, ESPG_3857));
              // add barriers to routeParameters
              routeParameters.getPointBarriers().add(pointBarrier);
              //[DocRef: Name=Route_And_Directions-Find_Route-Stops_And_Barriers

              // add route stops to the stops overlay
              SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, BLUE_COLOR, 14);
              routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stopMarker));
              routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stopMarker));
              SimpleMarkerSymbol barrierMarker = new SimpleMarkerSymbol(Style.CROSS, RED_COLOR, 14);
              routeGraphicsOverlay.getGraphics().add(new Graphic(pointBarrier.getGeometry(), barrierMarker));

              // add order text symbols to the stops
              TextSymbol stop1Text = new TextSymbol(10, "1", WHITE_COLOR, HorizontalAlignment.CENTER,
                  VerticalAlignment.MIDDLE);
              TextSymbol stop2Text = new TextSymbol(10, "2", WHITE_COLOR, HorizontalAlignment.CENTER,
                  VerticalAlignment.MIDDLE);
              routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stop1Text));
              routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stop2Text));
            } else {
              Platform.runLater(() -> {
                Alert dialog = new Alert(Alert.AlertType.ERROR);
                dialog.setHeaderText("Route Task Load Error");
                dialog.setContentText("Error: " + routeTask.getLoadError().getAdditionalMessage());
                dialog.showAndWait();
              });
            }

          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });

      } catch (Exception e) {
        e.printStackTrace();
      }

      // add the map view and control panel to stack pane
      stackPane.getChildren().addAll(mapView, vBoxControl);
      StackPane.setAlignment(vBoxControl, Pos.TOP_LEFT);
      StackPane.setMargin(vBoxControl, new Insets(10, 0, 0, 10));

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() throws Exception {

    if (mapView != null) {
      mapView.dispose();
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
