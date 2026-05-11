package com.lefestin.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central unit conversion utility used by DAO write paths and
 * service-level quantity matching.
 *
 * Strategy:
 * - Normalize stored values to base units per family for SQL consistency
 * - Convert at runtime when comparing values from different units
 * - Fall back safely when a unit is unknown
 */
public class ConversionService {

    private enum Family {
        MASS("gram"),
        VOLUME("milliliter"),
        COUNT("piece");

        private final String baseUnit;

        Family(String baseUnit) {
            this.baseUnit = baseUnit;
        }

        public String getBaseUnit() {
            return baseUnit;
        }
    }

    private static class UnitDef {
        private final Family family;
        private final double factorToBase;

        UnitDef(Family family, double factorToBase) {
            this.family = family;
            this.factorToBase = factorToBase;
        }
    }

    public static class NormalizedAmount {
        private final double quantity;
        private final String unit;

        public NormalizedAmount(double quantity, String unit) {
            this.quantity = quantity;
            this.unit = unit;
        }

        public double getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }
    }

    private final Map<String, UnitDef> unitMap = new HashMap<>();

    public ConversionService() {
        registerMassUnits();
        registerVolumeUnits();
        registerCountUnits();
    }

    /**
     * Normalizes quantity+unit to base storage units.
     * Unknown units are preserved but cleaned.
     */
    public NormalizedAmount normalize(double quantity, String unit) {
        String cleaned = cleanUnit(unit);
        UnitDef def = unitMap.get(cleaned);

        if (def == null) {
            return new NormalizedAmount(roundQty(quantity), cleaned);
        }

        double baseQty = quantity * def.factorToBase;
        return new NormalizedAmount(roundQty(baseQty), def.family.getBaseUnit());
    }

    /**
     * Attempts to convert quantity from one unit to another.
     * Returns null when units are incompatible.
     */
    public Double tryConvert(double quantity, String fromUnit, String toUnit) {
        String from = cleanUnit(fromUnit);
        String to = cleanUnit(toUnit);

        if (from.equalsIgnoreCase(to)) {
            return roundQty(quantity);
        }

        UnitDef fromDef = unitMap.get(from);
        UnitDef toDef = unitMap.get(to);

        if (fromDef == null || toDef == null) {
            return null;
        }
        if (fromDef.family != toDef.family) {
            return null;
        }

        double qtyInBase = quantity * fromDef.factorToBase;
        double converted = qtyInBase / toDef.factorToBase;
        return roundQty(converted);
    }

    public boolean canConvert(String fromUnit, String toUnit) {
        return tryConvert(1.0, fromUnit, toUnit) != null;
    }

    private void registerMassUnits() {
        register(Family.MASS, 1.0, "gram", "grams", "g");
        register(Family.MASS, 1000.0, "kilogram", "kilograms", "kg");
    }

    private void registerVolumeUnits() {
        register(Family.VOLUME, 1.0, "milliliter", "milliliters", "ml");
        register(Family.VOLUME, 1000.0, "liter", "liters", "l");
        register(Family.VOLUME, 5.0, "teaspoon", "teaspoons", "tsp");
        register(Family.VOLUME, 15.0, "tablespoon", "tablespoons", "tbsp");
        register(Family.VOLUME, 240.0, "cup", "cups");
    }

    private void registerCountUnits() {
        register(Family.COUNT, 1.0, "piece", "pieces");
        register(Family.COUNT, 1.0, "whole", "wholes");
        register(Family.COUNT, 1.0, "clove", "cloves");
        register(Family.COUNT, 1.0, "slice", "slices");
        register(Family.COUNT, 1.0, "pinch", "pinches");
    }

    private void register(Family family, double factorToBase, String... names) {
        for (String name : names) {
            unitMap.put(cleanUnit(name), new UnitDef(family, factorToBase));
        }
    }

    private String cleanUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "piece";
        }
        return unit.trim().toLowerCase(Locale.ROOT);
    }

    private double roundQty(double qty) {
        return Math.round(qty * 100.0) / 100.0;
    }
}
