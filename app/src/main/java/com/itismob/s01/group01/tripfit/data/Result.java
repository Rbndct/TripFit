package com.itismob.s01.group01.tripfit.data;

/** Success/error envelope for repository operations; never leaks a stack trace to the UI. */
public final class Result<T> {

    private final boolean success;
    private final T data;
    private final String errorMessage;

    private Result(boolean success, T data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Delivered on the main thread by {@link com.itismob.s01.group01.tripfit.data.AppExecutors}. */
    public interface Callback<T> {
        void onResult(Result<T> result);
    }
}
