package com.am.analysis.service;

/**
 * Live market tick used to overlay Mongo holdings without persisting each tick.
 */
public record LivePriceTick(double lastPrice, Double previousClose) {
}
