/* 
 * Original work: Copyright (c) 2010-2011 Brigham Young University
 * Modified work: Copyright (c) 2017 Xilinx, Inc. 
 * All rights reserved.
 *
 * Author: Chris Lavin, Xilinx Research Labs.
 *  
 * This file is part of RapidWright. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.xilinx.rapidwright.analysis;

import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QWidget;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.gui.TileView;


public class HeatMapBrowser extends QMainWindow{
	/** The Qt View for the browser */
	protected TileView view;
	/** The Qt Scene for the browser */
	protected HeatMapScene scene;
	/** The label for the status bar at the bottom */
	private QLabel statusLabel;
	/** The current device */
	private Device device;
	/** The current design */
	private Design design;
	/** This is the current tile that has been selected */
	private Tile currTile = null;
	
	protected boolean drawPrimitives = true; 

	/**
	 * Main method setting up the Qt environment for the program to run.
	 * @param args
	 */
	public static void main(String[] args){
		QApplication.setGraphicsSystem("raster");
		QApplication.initialize(args);
		HeatMapBrowser testPTB = new HeatMapBrowser(null, args);
		testPTB.show();
		QApplication.exec();
	}

	public HeatMapBrowser(QWidget parent, String[] args){
		super(parent);
		
		// set the title of the window
		setWindowTitle("Congestion Heat Map");
		
		Design design = Design.readCheckpoint(args[0]);
		setDesign(design);
		
		// Setup the scene and view for the GUI
		scene = new HeatMapScene(design, drawPrimitives);
		scene.setDevice(device);
		scene.initializeScene(drawPrimitives);
		view = new TileView(scene);
		setCentralWidget(view);

		// Setup some signals for when the user interacts with the view
		scene.updateStatus.connect(this, "updateStatus(String, Tile)");
		
		// Initialize the status bar at the bottom
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		QStatusBar statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);
		
	}

	/**
	 * This method updates the status bar each time the mouse moves from a 
	 * different tile.
	 */
	protected void updateStatus(String text, Tile tile){
		statusLabel.setText(text);
		currTile = tile;
	}

	/*
	 * Getters and Setters
	 */
	private void setDesign(Design design) {
		this.design = design;
		this.device = design.getDevice();
	}

}
