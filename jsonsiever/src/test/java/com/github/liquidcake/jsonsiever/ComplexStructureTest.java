/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package com.github.liquidcake.jsonsiever;

import org.junit.jupiter.api.Test;

public class ComplexStructureTest extends BaseTest {

    /**
     * Filter complex structure file: apply object fields filters to objects on different levels
     */
    @Test
    public void testComplexStructure_applyFieldFiltersOnDifferentLevels() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_applyFieldFiltersOnDifferentLevels.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_applyFieldFiltersOnDifferentLevels.json"
        );
    }

    /**
     * Filter complex structure file: apply object wildcard filters to objects on different levels
     */
    @Test
    public void testComplexStructure_applyObjectWildcardFiltersOnDifferentLevels() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_applyObjectWildcardFiltersOnDifferentLevels.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_applyObjectWildcardFiltersOnDifferentLevels.json"
        );
    }

    /**
     * Filter deep nested objects hierarchy
     */
    @Test
    public void testComplexStructure_deepObject() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_deepObject.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_deepObject.json"
        );
    }

    /**
     * Filter deep nested object hierarchy, filter object with wildcard on mid-depth
     */
    @Test
    public void testComplexStructure_deepObject_midDepthWildcard() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_deepObject_midDepthWildcard.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_deepObject_midDepthWildcard.json"
        );
    }

    /**
     * Filter complex structure file: apply array wildcard filters to arrays on high level
     */
    @Test
    public void testComplexStructure_applyArrayWildcardOnHighLevel() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_applyArrayWildcardOnHighLevel.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_applyArrayWildcardOnHighLevel.json"
        );
    }

    /**
     * Filter complex structure file: apply array wildcard filters to arrays on low level
     */
    @Test
    public void testComplexStructure_applyArrayWildcardOnLowLevel() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_applyArrayWildcardOnLowLevel.json",
                "/mock/assertions/ComplexStructureTest/testComplexStructure_applyArrayWildcardOnLowLevel.json"
        );
    }

    /**
     * Filter deep nested array hierarchy
     */
    @Test
    public void testComplexStructure_deepArray() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_deepArray.json",
                "/mock/assertions/ComplexStructureTest/deep_array.json"
        );
    }

    /**
     * Filter deep nested array hierarchy, filter array with wildcard on mid-depth
     */
    @Test
    public void testComplexStructure_deepArray_midDepthWildcard() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/ComplexStructureTest/complex_structure.json",
                "/mock/patterns/ComplexStructureTest/testComplexStructure_deepArray_midDepthWildcard.json",
                "/mock/assertions/ComplexStructureTest/deep_array.json"
        );
    }
}
