package de.markherrmann.javauno.exceptions;

public class FileReadException extends RuntimeException {
    public FileReadException() {
        super(ExceptionMessage.FILE_READ_ERROR.getValue());
    }
}
