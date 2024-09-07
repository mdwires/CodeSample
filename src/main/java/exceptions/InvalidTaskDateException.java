package exceptions;

public class InvalidTaskDateException extends Exception {
    public InvalidTaskDateException() {}

    public InvalidTaskDateException(String message) {
        super(message);
    }
}
