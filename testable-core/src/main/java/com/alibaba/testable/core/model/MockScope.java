package com.alibaba.testable.core.model;

public enum MockScope {

    /**
     * Mock method only available for test cases in the test class it belongs to
     */
    ASSOCIATED,

    /**
     * Mock method available for any test cases
     */
    GLOBAL

}
