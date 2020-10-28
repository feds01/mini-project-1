package client;

/**
 * Enum to represent the status of a download.
 *
 * @author 200008575
 * */
public enum DownloaderStatus {
    NOT_STARTED,
    STARTED,
    FINISHED,
    FAILED,
    FAILED_TIMEOUT,
    FAILED_MISMATCHING_SIGNATURE
}
