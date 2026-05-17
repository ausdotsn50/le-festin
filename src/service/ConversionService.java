package com.lefestin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central unit conversion utility used by DAO write paths and
 * service-level quantity matching.
 *
 * Strategy:
 * - Store the unit chosen by the UI unchanged
 * - Convert only through explicit, bidirectional rules within the same unit family
 * - Fail closed when no direct rule exists
 * - Prevent cross-family conversions (mass ≠ volume ≠ count)
 */
public class ConversionService {

    public enum UnitFamily {
        MASS("Mass (gram, kilogram)"),
        VOLUME("Volume (milliliter, liter, teaspoon, tablespoon, cup)"),
        COUNT("Count (piece, whole, clove, slice, pinch)");

        private final String label;

        UnitFamily(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, Map<String, Double>> conversionMap = new HashMap<>();
    private final Map<String, UnitFamily> unitFamilies = new HashMap<>();

    public ConversionService() {
        registerMassRules();
        registerVolumeRules();
        registerCountRules();
    }

    /**
     * Converts quantity from one unit into another.
     * Returns null when no direct, unambiguous rule exists.
     */
    public Double tryConvert(double quantity, String fromUnit, String toUnit) {
        String from = cleanUnit(fromUnit);
        String to = cleanUnit(toUnit);

        if (from.equalsIgnoreCase(to)) {
            return roundQty(quantity);
        }

        String canonicalFrom = aliases.getOrDefault(from, from);
        String canonicalTo = aliases.getOrDefault(to, to);

        // Runtime safety: if both units are known and belong to different
        // families, do not attempt conversion — fail closed.
        UnitFamily fromFamily = unitFamilies.get(canonicalFrom);
        UnitFamily toFamily = unitFamilies.get(canonicalTo);
        if (fromFamily != null && toFamily != null && fromFamily != toFamily) {
            return null;
        }

        Double factor = lookupFactor(canonicalFrom, canonicalTo);
        if (factor == null) {
            return null;
        }

        return roundQty(quantity * factor);
    }

    public boolean canConvert(String fromUnit, String toUnit) {
        return tryConvert(1.0, fromUnit, toUnit) != null;
    }

    /**
     * Returns all units that can be converted to the given unit.
     * Useful for UI suggestions.
     */
    public List<String> getCompatibleUnits(String unit) {
        String canonical = aliases.getOrDefault(cleanUnit(unit), cleanUnit(unit));
        Map<String, Double> targets = conversionMap.get(canonical);
        if (targets == null) return new ArrayList<>();

        // Filter compatible units to the same unit family when possible
        UnitFamily fam = unitFamilies.get(canonical);
        List<String> out = new ArrayList<>();
        for (String t : targets.keySet()) {
            if (fam == null) {
                out.add(t);
            } else {
                UnitFamily tf = unitFamilies.get(t);
                if (tf == fam) out.add(t);
            }
        }
        return out;
    }

    /**
     * Returns the unit family (MASS, VOLUME, COUNT) for a given unit.
     * Returns null if unit is unknown or custom.
     */
    public UnitFamily getUnitFamily(String unit) {
        String canonical = aliases.getOrDefault(cleanUnit(unit), cleanUnit(unit));
        return unitFamilies.get(canonical);
    }

    private void registerMassRules() {
        registerAlias("gram");
        registerAlias("kilogram");
        
        registerFamily(UnitFamily.MASS, "gram", "kilogram");
        
        registerBidirectional("gram", "kilogram", 0.001);
    }

    private void registerVolumeRules() {
        registerAlias("milliliter");
        registerAlias("liter");
        registerAlias("teaspoon");
        registerAlias("tablespoon");
        registerAlias("cup");

        registerFamily(UnitFamily.VOLUME, "milliliter", "liter", "teaspoon", 
                       "tablespoon", "cup");

        registerBidirectional("milliliter", "liter", 0.001);
        registerBidirectional("milliliter", "teaspoon", 0.2);
        registerBidirectional("milliliter", "tablespoon", 0.0666666667);
        registerBidirectional("milliliter", "cup", 0.0041666667);
        registerBidirectional("liter", "teaspoon", 200.0);
        registerBidirectional("liter", "tablespoon", 66.6666667);
        registerBidirectional("liter", "cup", 4.1666667);
        registerBidirectional("teaspoon", "tablespoon", 0.3333333333);
        registerBidirectional("teaspoon", "cup", 0.0208333333);
        registerBidirectional("tablespoon", "cup", 0.0625);
    }

    private void registerCountRules() {
        registerAlias("piece");
        registerAlias("whole");
        registerAlias("clove");
        registerAlias("slice");
        registerAlias("pinch");

        registerFamily(UnitFamily.COUNT, "piece", "whole", "clove", "slice", "pinch");
        
        // IMPORTANT: Do NOT register cross-conversions between different count units.
        // 1 clove garlic ≠ 1 whole egg ≠ 1 slice bread
        // Count units are semantically incompatible and should not merge.
        // Only aliases (piece ↔ pieces) work within the same unit.
    }

    private void registerAlias(String canonical, String... synonyms) {
        String cleanedCanonical = cleanUnit(canonical);
        aliases.put(cleanedCanonical, cleanedCanonical);
        for (String synonym : synonyms) {
            aliases.put(cleanUnit(synonym), cleanedCanonical);
        }
    }

    /**
     * Mark units as belonging to a specific family (MASS, VOLUME, COUNT).
     * Prevents accidental cross-family conversions.
     */
    private void registerFamily(UnitFamily family, String... units) {
        for (String unit : units) {
            String canonical = cleanUnit(unit);
            unitFamilies.put(canonical, family);
        }
    }

    private void registerBidirectional(String from, String to, double factorFromTo) {
        String cleanedFrom = cleanUnit(from);
        String cleanedTo = cleanUnit(to);
        
        String canonicalFrom = aliases.getOrDefault(cleanedFrom, cleanedFrom);
        String canonicalTo = aliases.getOrDefault(cleanedTo, cleanedTo);

        // Validation: both units must be in the same family
        UnitFamily fromFamily = unitFamilies.get(canonicalFrom);
        UnitFamily toFamily = unitFamilies.get(canonicalTo);
        
        if (fromFamily != null && toFamily != null && fromFamily != toFamily) {
            throw new IllegalArgumentException(
                "Cannot register conversion between " + canonicalFrom + " (" + fromFamily + ") " +
                "and " + canonicalTo + " (" + toFamily + ") — different unit families");
        }

        conversionMap
            .computeIfAbsent(canonicalFrom, key -> new HashMap<>())
            .put(canonicalTo, factorFromTo);

        conversionMap
            .computeIfAbsent(canonicalTo, key -> new HashMap<>())
            .put(canonicalFrom, 1.0 / factorFromTo);
    }

    private Double lookupFactor(String from, String to) {
        Map<String, Double> targets = conversionMap.get(from);
        if (targets == null) {
            return null;
        }
        return targets.get(to);
    }

    private String cleanUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "";
        }
        return unit.trim().toLowerCase(Locale.ROOT);
    }

    private double roundQty(double qty) {
        return Math.round(qty * 100.0) / 100.0;
    }
}
