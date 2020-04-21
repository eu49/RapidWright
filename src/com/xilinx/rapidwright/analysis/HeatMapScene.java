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

import java.util.HashSet;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.QSizeF;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsPixmapItem;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QImage.Format;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.util.Utils;
import com.xilinx.rapidwright.gui.GUIModuleInst;
import com.xilinx.rapidwright.gui.TileScene;


public class HeatMapScene extends TileScene{
	private double[][] congestion;
	private Design design;
	private QSize sceneSize;

	public HeatMapScene(){
		setDesign(null);
		initializeScene(true);
	}
	
	public HeatMapScene(Design design, boolean drawPrimitives){
		setDesign(design);
		initializeScene(drawPrimitives);
	}
	
	@SuppressWarnings("unchecked")
	public void initializeScene(boolean drawPrimitives){
		this.clear();
		prevX = 0;
		prevY = 0;

		// Used to avoid a bug in Qt
		System.gc();

		if(device != null){
			rows = device.getRows();
			cols = device.getColumns();
			sceneSize = new QSize((cols + 1) * (tileSize + 1), (rows + 1) * (tileSize + 1));
			setSceneRect(new QRectF(new QPointF(0, 0), new QSizeF(sceneSize)));
			drawFPGAFabric(drawPrimitives);
		}
		else{
			setSceneRect(new QRectF(0, 0, tileSize + 1, tileSize + 1));
		}
	}

	private void drawFPGAFabric(boolean drawPrimitives) {
		setBackgroundBrush(new QBrush(QColor.black));

		//Create transparent QPixmap that accepts hovers 
		//  so that moveMouseEvent is triggered
		QPixmap pixelMap = new QPixmap(sceneSize);
		pixelMap.fill(QColor.transparent);
		QGraphicsPixmapItem background = addPixmap(pixelMap);
		background.setAcceptsHoverEvents(true);
		background.setZValue(-1);

		// Draw colored tiles onto QPixMap
		qImage = new QImage(sceneSize, Format.Format_RGB16);
		QPainter painter = new QPainter(qImage);

		// Draw the tile layout
		int offset = (int) Math.ceil((lineWidth / 2.0));

		for(int y = 0; y < rows; y++){
			for(int x = 0; x < cols; x++){
				Tile tile = device.getTile(y, x);
				TileTypeEnum tileTypeEnum = tile.getTileTypeEnum();

				// Set pen color based on congestion
				double tileCongestion = congestion[tile.getRow()][tile.getColumn()];
				QColor color;
				if (tileCongestion < 70) {
					color = new QColor(255, 255, 255);
				} else if (tileCongestion < 80) {
					color = new QColor(255, 255, 0);
				} else if (tileCongestion < 90) {
					color = new QColor(255, 153, 0);
				} else {
					color = new QColor(255, 0, 0);
				}

				painter.setPen(color);

				int rectX = x * tileSize;
				int rectY = y * tileSize;
				int rectSide = tileSize - 2 * offset;

				if(drawPrimitives){
					if(Utils.isCLB(tileTypeEnum)) {
						drawCLB(painter, rectX, rectY, rectSide, color);
					} else if(Utils.isSwitchBox(tileTypeEnum)) {
						drawSwitchBox(painter, rectX, rectY, rectSide, color);
					} else if(Utils.isBRAM(tileTypeEnum)) {
						drawBRAM(painter, rectX, rectY, rectSide, offset, color);
					} else if(Utils.isDSP(tileTypeEnum)) {
						drawDSP(painter, rectX, rectY, rectSide, offset, color);
					} else {
						colorTile(painter, x, y, offset, color);
					}
				} else {
					colorTile(painter, x, y, offset, color);
				}
			}
		}

		painter.end();
	}

	/**
	 * Gets the tile based on the x and y coordinates given (typically from mouse input)
	 * @param x The x location on the screen.
	 * @param y The y location on the screen.
	 * @return The tile at the x,y location or null if none exist.
	 */
	@Override
	public Tile getTile(double x, double y){
		currX = (int) Math.floor(x / tileSize);
		currY = (int) Math.floor(y / tileSize);
		if (currX >= 0 && currY >= 0 && currX < cols && currY < rows){// && (currX != prevX || currY != prevY)){
			return device.getTile(currY, currX);
		}
		return null;
	}
	
	/*
	 * Getters and Setters
	 */
	@Override
	public void setDesign(Design design){
		this.design = design;
		if(this.design != null){
			setDevice(design.getDevice());
			int numRows = device.getRows();
			int numCols = device.getColumns();
			Collection<Net> allNets = design.getNets();
			Set<PIP> usedPIPs = new HashSet<PIP>();
			for (Net net : allNets) {
				List<PIP> netPIPs = net.getPIPs();
				usedPIPs.addAll(netPIPs);
			}
			double[][] congestion = new double[numRows][numCols];
			for (PIP pip : usedPIPs) {
				Tile pipTile = pip.getTile();
				congestion[pipTile.getRow()][pipTile.getColumn()] += 100.0d / pipTile.getPIPs().size();
			}
			setCongestion(congestion);
		}
	}

	public double[][] getCongestion() {
		return congestion;
	}

	public void setCongestion(double[][] congestion) {
		this.congestion = congestion;
	}

	/*
	 * Helper Drawing Methods
	 */
	private void drawCLB(QPainter painter, int rectX, int rectY, int rectSide, QColor color){
		switch(device.getSeries()){
			case Series7:
				painter.fillRect(rectX, rectY + rectSide / 2, rectSide / 2 - 1, rectSide / 2 - 1, new QColor(color));
				painter.fillRect(rectX + rectSide / 2, rectY, rectSide / 2 - 1, rectSide / 2 - 1, new QColor(color));
				break;
			default:
				painter.fillRect(rectX, rectY, rectSide, rectSide, new QColor(color));
		}
	}

	private void drawBRAM(QPainter painter, int rectX, int rectY, int rectSide, int offset, QColor color){
		switch(device.getSeries()){
			case Series7:
			case UltraScale:
			case UltraScalePlus:
				painter.fillRect(rectX, rectY - 4 * tileSize, rectSide - 1, 5 * rectSide + 3 * 2 * offset - 1, new QColor(color));
				painter.setPen(color.darker());
				painter.fillRect(rectX+2, rectY-4 * tileSize + 2, rectSide - 5, ((int)(2.5 * rectSide)) + 3 * 2 * offset - 5, new QColor(color));
				painter.fillRect(rectX+2, (rectY-2 * tileSize) + 7, rectSide - 5, ((int)(2.5 * rectSide)) + 3 * 2 * offset - 5, new QColor(color));
				break;
		}
	}

	private void drawDSP(QPainter painter, int rectX, int rectY, int rectSide, int offset, QColor color){
		switch(device.getSeries()){
			case Series7:
			case UltraScale:
			case UltraScalePlus:
				painter.fillRect(rectX, rectY - 4 * tileSize, rectSide - 1, 5 * rectSide + 3 * 2 * offset - 1, new QColor(color));
				painter.setPen(color.darker());
				painter.fillRect(rectX+2, rectY-4 * tileSize + 2, rectSide - 5, ((int)(2.5 * rectSide)) + 3 * 2 * offset - 5, new QColor(color));
				painter.fillRect(rectX+2, (rectY-2 * tileSize) + 7, rectSide - 5, ((int)(2.5 * rectSide)) + 3 * 2 * offset - 5, new QColor(color));
				break;
		}
	}

	private void drawSwitchBox(QPainter painter, int rectX, int rectY, int rectSide, QColor color){
		painter.fillRect(rectX + rectSide / 6, rectY, 4 * rectSide / 6 - 1, rectSide - 1, new QColor(color));
	}

	private void colorTile(QPainter painter, int x, int y, int offset, QColor color){
		painter.fillRect(x * tileSize, y * tileSize, tileSize - 2 * offset, tileSize - 2 * offset, new QColor(color));
	}

}
