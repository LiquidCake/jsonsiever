/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever.core.exception;

public class JsonFilteringException extends Exception {

    public JsonFilteringException(String message) {
        super(message);
    }

    public JsonFilteringException(Throwable cause) {
        super(cause);
    }

    public JsonFilteringException(String message, Throwable cause) {
        super(message, cause);
    }
}
