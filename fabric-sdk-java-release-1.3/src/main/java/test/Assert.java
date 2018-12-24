package test;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//



public class Assert {
    protected Assert() {
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }

    }

    public static void assertTrue(boolean condition) {
        assertTrue((String)null, condition);
    }

    public static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    public static void assertFalse(boolean condition) {
        assertFalse((String)null, condition);
    }

    public static void fail(String message) {
        if (message == null) {
            throw new AssertionError();
        } else {
            throw new AssertionError(message);
        }
    }

    public static void fail() {
        fail((String)null);
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        if (!equalsRegardingNull(expected, actual)) {
            if (expected instanceof String && actual instanceof String) {
                String cleanMessage = message == null ? "" : message;
                //throw new ComparisonFailure(cleanMessage, (String)expected, (String)actual);
            } else {
                failNotEquals(message, expected, actual);
            }
        }
    }

    private static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        } else {
            return isEquals(expected, actual);
        }
    }

    private static boolean isEquals(Object expected, Object actual) {
        return expected.equals(actual);
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals((String)null, (Object)expected, (Object)actual);
    }

    public static void assertNotEquals(String message, Object unexpected, Object actual) {
        if (equalsRegardingNull(unexpected, actual)) {
            failEquals(message, actual);
        }

    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        assertNotEquals((String)null, unexpected, actual);
    }

    private static void failEquals(String message, Object actual) {
        String formatted = "Values should be different. ";
        if (message != null) {
            formatted = message + ". ";
        }

        formatted = formatted + "Actual: " + actual;
        fail(formatted);
    }

    public static void assertNotEquals(String message, long unexpected, long actual) {
        if (unexpected == actual) {
            failEquals(message, actual);
        }

    }

    public static void assertNotEquals(long unexpected, long actual) {
        assertNotEquals((String)null, unexpected, actual);
    }

    public static void assertNotEquals(String message, double unexpected, double actual, double delta) {
        if (!doubleIsDifferent(unexpected, actual, delta)) {
            failEquals(message, actual);
        }

    }

    public static void assertNotEquals(double unexpected, double actual, double delta) {
        assertNotEquals((String)null, unexpected, actual, delta);
    }

    public static void assertNotEquals(float unexpected, float actual, float delta) {
        assertNotEquals((String)null, unexpected, actual, delta);
    }




    public static void assertEquals(String message, double expected, double actual, double delta) {
        if (doubleIsDifferent(expected, actual, delta)) {
            failNotEquals(message, expected, actual);
        }

    }

    public static void assertEquals(String message, float expected, float actual, float delta) {
        if (floatIsDifferent(expected, actual, delta)) {
            failNotEquals(message, expected, actual);
        }

    }

    public static void assertNotEquals(String message, float unexpected, float actual, float delta) {
        if (!floatIsDifferent(unexpected, actual, delta)) {
            failEquals(message, actual);
        }

    }

    private static boolean doubleIsDifferent(double d1, double d2, double delta) {
        if (Double.compare(d1, d2) == 0) {
            return false;
        } else {
            return Math.abs(d1 - d2) > delta;
        }
    }

    private static boolean floatIsDifferent(float f1, float f2, float delta) {
        if (Float.compare(f1, f2) == 0) {
            return false;
        } else {
            return Math.abs(f1 - f2) > delta;
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals((String)null, expected, actual);
    }

    public static void assertEquals(String message, long expected, long actual) {
        if (expected != actual) {
            failNotEquals(message, expected, actual);
        }

    }

    /** @deprecated */
    @Deprecated
    public static void assertEquals(double expected, double actual) {
        assertEquals((String)null, expected, actual);
    }

    /** @deprecated */
    @Deprecated
    public static void assertEquals(String message, double expected, double actual) {
        fail("Use assertEquals(expected, actual, delta) to compare floating-point numbers");
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals((String)null, expected, actual, delta);
    }

    public static void assertEquals(float expected, float actual, float delta) {
        assertEquals((String)null, expected, actual, delta);
    }

    public static void assertNotNull(String message, Object object) {
        assertTrue(message, object != null);
    }

    public static void assertNotNull(Object object) {
        assertNotNull((String)null, object);
    }

    public static void assertNull(String message, Object object) {
        if (object != null) {
            failNotNull(message, object);
        }
    }

    public static void assertNull(Object object) {
        assertNull((String)null, object);
    }

    private static void failNotNull(String message, Object actual) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }

        fail(formatted + "expected null, but was:<" + actual + ">");
    }

    public static void assertSame(String message, Object expected, Object actual) {
        if (expected != actual) {
            failNotSame(message, expected, actual);
        }
    }

    public static void assertSame(Object expected, Object actual) {
        assertSame((String)null, expected, actual);
    }

    public static void assertNotSame(String message, Object unexpected, Object actual) {
        if (unexpected == actual) {
            failSame(message);
        }

    }

    public static void assertNotSame(Object unexpected, Object actual) {
        assertNotSame((String)null, unexpected, actual);
    }

    private static void failSame(String message) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }

        fail(formatted + "expected not same");
    }

    private static void failNotSame(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }

        fail(formatted + "expected same:<" + expected + "> was not:<" + actual + ">");
    }

    private static void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual));
    }

    static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !message.equals("")) {
            formatted = message + " ";
        }

        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        return expectedString.equals(actualString) ? formatted + "expected: " + formatClassAndValue(expected, expectedString) + " but was: " + formatClassAndValue(actual, actualString) : formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
    }

    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }

//    /** @deprecated */
//    @Deprecated
//    public static void assertEquals(String message, Object[] expecteds, Object[] actuals) {
//        assertArrayEquals(message, expecteds, actuals);
//    }
//
//    /** @deprecated */
//    @Deprecated
//    public static void assertEquals(Object[] expecteds, Object[] actuals) {
//        assertArrayEquals(expecteds, actuals);
//    }
//
//    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
//        assertThat("", actual, matcher);
//    }
//
//    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
//        MatcherAssert.assertThat(reason, actual, matcher);
//    }
}
