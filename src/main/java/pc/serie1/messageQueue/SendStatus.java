package pc.serie1.messageQueue;

public interface SendStatus {
    boolean isSent();
    boolean tryCancel();
    boolean await(int timeout) throws InterruptedException;
}
